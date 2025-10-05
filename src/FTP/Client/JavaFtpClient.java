package FTP.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import FTP.Util.Util;

/**
 * @author Eduardo Díaz Sánchez
 */
public class JavaFtpClient {
	
	public static void main(String[] args) {
		FTPClient ftpClient;
		ClientFuntions clientFuntions;
		String[] hostname, userCredentials;
		int replyCode, dataPort, option;
		boolean login;
		String mode;
		boolean exit = false;
		
		System.out.println("Cliente FTP iniciado");
		
		ftpClient = new FTPClient();
		clientFuntions = new ClientFuntions(ftpClient);
		
		try (Scanner sc = new Scanner(System.in)) {
			hostname = captureHostname(sc);
		
			ftpClient.connect(hostname[0], Integer.parseInt(hostname[1]));
			replyCode = ftpClient.getReplyCode();			
			if (!FTPReply.isPositiveCompletion(replyCode)) {
				throw new IOException("Error de conexión: " + replyCode);
			}
			
			System.out.println("\n" + ftpClient.getReplyString());

			userCredentials = inputUserCredentials(sc);
			login = ftpClient.login(userCredentials[0], userCredentials[1]);
			if (!login) {
				replyCode = ftpClient.getReplyCode();	
				throw new SecurityException("Autenticación fallida: " + replyCode);
			} 
			
			Util.printGreenColor("\n" + ftpClient.getReplyString());
			
			mode = selectMode(sc);
			if (mode.equals("PASSIVE")) {
				handlerPasiveMode(ftpClient, sc);
			} else if (mode.equals("ACTIVE")) {
				dataPort = handlerActiveMode(ftpClient, sc);
			} else {
				Util.printRedColor("Error: Modo de conexión elegido desconocido");
			}
			
			ftpClient.sendCommand("SYST"); // Llamamos a SYST manualmente
			System.out.println("\nSYST Response: " + ftpClient.getReplyString());
			
			do {
				showMenu();
			    System.out.print("\nSeleccione una opción \n:> ");
				
				try {
					option = Integer.parseInt(sc.nextLine().trim());
				} catch (NumberFormatException e) {
					option = -1;
				}

				switch (option) {
					case 1:
						System.out.println("\nListar archivos");
						clientFuntions.listFiles();
						break;
						
					case 2:
					    System.out.println("\nSubir archivo");
					    clientFuntions.uploadFile(sc);
						break;
						
					case 3:
					    System.out.println("\nDescargar archivo");
					    clientFuntions.downloadFile(sc);
						break;
						
					case 4:
					    System.out.println("\nEliminar archivo");
					    clientFuntions.deleteFile(sc);
						break;
						
					case 5:
					    System.out.println("\nCrear directorio");
					    clientFuntions.createDirectory(sc);
					    break;

					case 6:
					    System.out.println("\nEliminar directorio");
					    clientFuntions.deleteDirectory(sc);
					    break;
						
					case 7:
					    System.out.println("\nRenombrar archivo/directorio");
					    clientFuntions.renameFile(sc);
					    break;
						
					case 8:
					    System.out.println("\nCambiar directorio de trabajo");
					    clientFuntions.changeWorkingDirectory(sc);
						break;
						
					case 9:
					    System.out.println("\nCambiar al directorio superior");
					    clientFuntions.changeToParentDirectory();
						break;
						
					case 10:
						System.out.println("\nSaliendo del cliente FTP");
						exit = true;
						break;
					default:
						Util.printRedColor("\nOpción no válida");
						break;
				}
				
			} while (!exit);
			
		} catch (IOException e) {
			 System.out.println("Error: " + e.getMessage());
		} finally {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                System.out.println("Error al cerrar conexión: " + e.getMessage());
            }
		}
	}
	
	private static String[] captureHostname(Scanner sc) {
		String hostname = "localhost"; 
		String port = "21";
		String input = null;
		
		System.out.println("\nIntroduce el hostname del servidor FTP (localhost por defecto)");
		System.out.print(":> ");
		input = sc.nextLine().trim();
		if (input != null && !input.isBlank())
			hostname = input;

		System.out.println("\nIntroduce el puerto (puerto 21 por defecto)");
		System.out.print(":> ");
		input = sc.nextLine().trim();
		if (input != null && !input.isBlank())
			port = input;
		
		return new String[] {hostname, port};		
	}
	
	private static String[] inputUserCredentials(Scanner sc) {
		String input = null;
		String user = "";
		String pass = "";
		
		do {
			System.out.println("\nIntroduce el nombre de usuario");
			System.out.print(":> ");
			input = sc.nextLine().trim();
			
			if (input.isEmpty() || input.isBlank()) {
				Util.printRedColor("\nError: El nombre de usuario no puede estar vacío");
				input = null;
			}	
			
		} while (input == null);
		
		user = input;
		
		do {
			System.out.println("\nIntroduce la contraseña");
			System.out.print(":> ");
			input = sc.nextLine().trim();
			
			if (input.isEmpty() || input.isBlank()) {
				Util.printRedColor("Error: La contraseña no puede estar vacía");
				input = null;
			}	
			
		} while (input == null);
		
		pass = input;
		
		return new String[] {user, pass};
	}
	
	private static String selectMode(Scanner sc) {
		String mode = "PASIVE";
		String input = null;
		
		do {
			System.out.println("Introduce el modo de conexión PASSIVE/ACTIVE");
			System.out.print(":> ");
			input = sc.nextLine().trim();
			
			if (input.isEmpty() || input.isBlank()) {
				Util.printRedColor("\nError: El campo no puede estar vacío, elige un modo de conexión: PASSIVE/ACTIVE");
				input = null;
			} else if (!input.toUpperCase().matches("PASSIVE|ACTIVE")) {
				Util.printRedColor("\nError: Debe elegir un modo de conexión CORECTO: PASSIVE/ACTIVE");
				input = null;
			}
			
		} while (input == null);
		
		mode = input.toUpperCase();
		
		return mode;
	}
	
	public static void handlerPasiveMode(FTPClient ftpClient, Scanner sc) throws IOException {
		ftpClient.enterLocalPassiveMode();
		ftpClient.sendCommand("PASV");
		
		Util.printGreenColor("\nModo pasivo configurado");  
	}
	
	public static int handlerActiveMode(FTPClient ftpClient, Scanner sc) throws IOException {
		InetAddress inetAdress;
		String hostAddress, comandoPort;
		int dataPort, p1, p2;
		
		// Obtener la dirección IP local del cliente
		inetAdress = ftpClient.getLocalAddress();
		hostAddress = inetAdress.getHostAddress().replace(".", ",");
		
		// Pedir al usuario que elija un puerto para la conexión de datos
        System.out.println("\nPuerto para datos (ej. 5500 o superior)");  
        System.out.print(":> ");
        dataPort = Integer.parseInt(sc.nextLine());  

        // Calcular la parte alta y baja del puerto
        p1 = dataPort / 256;  
        p2 = dataPort % 256;  

        // Construir el comando PORT con la IP del cliente y el puerto
        comandoPort = String.format("PORT %s,%d,%d", hostAddress, p1, p2); 
        
        // Cambiar a modo activo
        ftpClient.enterLocalActiveMode(); 
        // Enviar manualmente el comando PORT al servidor
        ftpClient.sendCommand(comandoPort); 
        Util.printGreenColor("\nModo activo configurado"); 
        
        return dataPort; // Devuelve el puerto que el cliente abrió
	}
	
	public static void showMenu() {
	    System.out.println("\n=== Menú FTP ===");
	    System.out.println("[1] Listar archivos");
	    System.out.println("[2] Subir archivo");
	    System.out.println("[3] Descargar archivo");
	    System.out.println("[4] Eliminar archivo");
	    System.out.println("[5] Crear directorio");
	    System.out.println("[6] Eliminar directorio");
	    System.out.println("[7] Renombrar archivo/directorio");
	    System.out.println("[8] Cambiar directorio de trabajo");
	    System.out.println("[9] Cambiar al directorio superior");
	    System.out.println("[10] Salir");
	}
}
