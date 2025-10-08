package FTP.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import FTP.Util.Util;

/**
 * Cliente FTP interactivo de consola.
 * <p>
 * Proporciona una interfaz de línea de comandos para conectarse a un servidor FTP
 * y realizar operaciones como:
 * <ul>
 *   <li>Listar, subir y descargar archivos</li>
 *   <li>Crear y eliminar directorios</li>
 *   <li>Renombrar archivos y directorios</li>
 *   <li>Navegar por el sistema de archivos remoto</li>
 * </ul>
 * <p>
 * Soporta modos de transferencia ACTIVE y PASSIVE.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class JavaFtpClient {

	/**
	 * Punto de entrada principal del cliente FTP.
	 * Establece conexión con el servidor, autentica al usuario y presenta un menú interactivo.
	 *
	 * @param args Argumentos de línea de comandos (no utilizados)
	 */
	public static void main(String[] args) {
		FTPClient ftpClient;
		ClientFunctions clientFunctions;
		String[] hostname, userCredentials;
		int replyCode, dataPort, option;
		boolean login;
		String mode;
		boolean exit = false;

		System.out.println("Cliente FTP iniciado");

		ftpClient = new FTPClient();
		clientFunctions = new ClientFunctions(ftpClient);
		
		try (Scanner sc = new Scanner(System.in)) {
			hostname = captureHostname(sc);

			// Configurar encoding UTF-8 para soportar acentos, ñ, etc.
			ftpClient.setControlEncoding("UTF-8");

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

			// Configurar modo binario (crítico para PDFs, ZIPs, imágenes)
			ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
			Util.printGreenColor("\nModo de transferencia binaria activado");

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
						clientFunctions.listFiles();
						break;

					case 2:
					    System.out.println("\nSubir archivo");
					    clientFunctions.uploadFile(sc);
						break;

					case 3:
					    System.out.println("\nDescargar archivo");
					    clientFunctions.downloadFile(sc);
						break;

					case 4:
					    System.out.println("\nEliminar archivo");
					    clientFunctions.deleteFile(sc);
						break;

					case 5:
					    System.out.println("\nCrear directorio");
					    clientFunctions.createDirectory(sc);
					    break;

					case 6:
					    System.out.println("\nEliminar directorio");
					    clientFunctions.deleteDirectory(sc);
					    break;

					case 7:
					    System.out.println("\nRenombrar archivo/directorio");
					    clientFunctions.renameFile(sc);
					    break;

					case 8:
					    System.out.println("\nCambiar directorio de trabajo");
					    clientFunctions.changeWorkingDirectory(sc);
						break;

					case 9:
					    System.out.println("\nCambiar al directorio superior");
					    clientFunctions.changeToParentDirectory();
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

	/**
	 * Solicita y captura el hostname y puerto del servidor FTP.
	 *
	 * @param sc Scanner para leer entrada del usuario
	 * @return Array con [hostname, puerto]
	 */
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

	/**
	 * Solicita las credenciales del usuario (nombre y contraseña).
	 *
	 * @param sc Scanner para leer entrada del usuario
	 * @return Array con [username, password]
	 */
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

	/**
	 * Solicita al usuario seleccionar el modo de transferencia de datos.
	 *
	 * @param sc Scanner para leer entrada del usuario
	 * @return "PASSIVE" o "ACTIVE"
	 */
	private static String selectMode(Scanner sc) {
		String mode = "PASSIVE";
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

	/**
	 * Configura el cliente FTP en modo pasivo.
	 * En este modo, el servidor abre un puerto y el cliente se conecta a él.
	 *
	 * @param ftpClient Cliente FTP a configurar
	 * @param sc Scanner para entrada del usuario (no utilizado en modo pasivo)
	 * @throws IOException Si ocurre un error de comunicación
	 */
	public static void handlerPasiveMode(FTPClient ftpClient, Scanner sc) throws IOException {
		ftpClient.enterLocalPassiveMode();
		ftpClient.sendCommand("PASV");
		
		Util.printGreenColor("\nModo pasivo configurado");  
	}

	/**
	 * Configura el cliente FTP en modo activo.
	 * En este modo, el cliente abre un puerto y el servidor se conecta a él.
	 *
	 * @param ftpClient Cliente FTP a configurar
	 * @param sc Scanner para solicitar puerto de datos al usuario
	 * @return Puerto de datos abierto por el cliente
	 * @throws IOException Si ocurre un error de comunicación
	 */
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

	/**
	 * Muestra el menú de opciones disponibles para el cliente FTP.
	 */
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
