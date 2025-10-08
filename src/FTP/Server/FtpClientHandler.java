package FTP.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import FTP.Util.Util;

/**
 * Manejador de conexiones de clientes FTP.
 * <p>
 * Esta clase implementa {@link Runnable} para permitir el manejo concurrente de múltiples
 * clientes FTP. Cada instancia maneja una conexión de cliente completa, incluyendo:
 * <ul>
 *   <li>Autenticación de usuarios</li>
 *   <li>Procesamiento de comandos FTP</li>
 *   <li>Gestión de conexiones de datos (ACTIVE/PASSIVE)</li>
 *   <li>Control de acceso basado en roles (RBAC)</li>
 * </ul>
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class FtpClientHandler implements Runnable {
	/** Socket de control para comandos FTP */
	private Socket controlSocket;

	/** Modo de conexión actual: "ACTIVE" o "PASSIVE" */
	private String connectionMode;

	/** Socket del servidor para modo pasivo */
	private ServerSocket passiveDataSocket;

	/** Dirección IP para conexión de datos en modo pasivo */
	private String passiveDataIp;

	/** Puerto para conexión de datos en modo pasivo */
	private Integer passiveDataPort;

	/** Socket de datos para modo activo */
	private Socket activeDataSocket;

	/** Dirección IP del cliente para modo activo */
	private String activeDataIp;

	/** Puerto del cliente para modo activo */
	private Integer activeDataPort;

	/** BufferedReader para leer comandos del cliente */
	private BufferedReader in;

	/** PrintWriter para enviar respuestas al cliente */
	private PrintWriter out;

	/** Buffer temporal para almacenar nombre de usuario durante autenticación */
	private String usernameBuffer = null;

	/** Usuario actualmente autenticado */
	private User currentUser;

	/** Último comando recibido */
	private String command;

	/** Instancia de funciones del servidor FTP */
	private ServerFunctions serverFunctions;

	/** Directorio de trabajo actual del cliente */
	private String currentDirectory;

	/** Tipo de transferencia actual: "A" (ASCII) o "I" (Binary/Image) */
	private String transferType = "A";

	/**
	 * Constructor del manejador de cliente.
	 *
	 * @param controlSocket Socket de control establecido con el cliente
	 * @throws IOException Si ocurre un error al inicializar el manejador
	 */
	public FtpClientHandler(Socket controlSocket) throws IOException {
		this.controlSocket = controlSocket;
		currentUser = new User();
		serverFunctions = new ServerFunctions(this);
		currentDirectory = JavaFtpServer.dirRoot;
	}

	/**
	 * Método principal de ejecución del hilo.
	 * Procesa los comandos FTP del cliente en un bucle hasta que se cierre la conexión.
	 */
	@Override
	public void run() {
		System.out.println("\n[SOLICITUD RECIBIDA]");
		System.out.println("\nConexión con el cliente " + controlSocket.getInetAddress());

		try {
			// Usar UTF-8 explícitamente para soportar nombres con acentos, ñ, etc.
			in = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), "UTF-8"));
			out = new PrintWriter(controlSocket.getOutputStream(), true);

			sendReply(220, "Welcome to the FTP server");

			while ((command = in.readLine()) != null) {
				System.out.println("\nModo conexión: " +  ((connectionMode != null) ? connectionMode : "NO ESPECIFICADO"));
				System.out.println("Tipo usuario: " + currentUser.getProfile());
				System.out.println("Comando recibido: " + command);

				String[] commandParts = command.split(" ", 2); // Límite 2: comando + resto
			    String commandName = commandParts[0];
			    String commandArg = (commandParts.length > 1) ? commandParts[1] : null; // Toma TODO después del espacio
				
				switch (commandName) {
			        case "SYST":
			            sendReply(215, "UNIX Type: L8");
			            break;

					case "TYPE":
						handleTypeCommand(commandArg);
						break;

					case "USER":
						handleUserCommand(commandArg);
						break;
						
					case "PASS":
						handlePassCommand(commandArg);
						break;
						
					case "PASV":
						if (checkAuthentication(UserProfile.BASICO, UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR))
							handlePasvCommand();
						break;
						
					case "PORT":
						if (checkAuthentication(UserProfile.BASICO, UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR)) 
							handlePortCommand(commandArg);
						break;
						
					case "LIST":
				        if (checkAuthentication(UserProfile.BASICO, UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR)) 
				        	serverFunctions.handleListCommand();
						break;
						
					case "STOR":
					    if (checkAuthentication(UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR))
					    	serverFunctions.handleUploadFileCommand(commandArg);
					    break;
					    
					case "RETR":
					    if (checkAuthentication(UserProfile.BASICO, UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR))
					    	serverFunctions.handleDownloadFileCommand(commandArg);
					    break;
					    
					case "DELE":
					    if (checkAuthentication(UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR))
					        serverFunctions.handleDeleteFileCommand(commandArg);
					    break;
					    
					case "MKD":
				        if (checkAuthentication(UserProfile.ADMINISTRADOR)) 
				        	serverFunctions.handleCreateDirectory(commandArg);
					    break;
					    
				    case "RMD":
				    	if (checkAuthentication(UserProfile.ADMINISTRADOR))
				    		serverFunctions.handleDeleteDirectoryCommand(commandArg);
				        break;
				        
				    case "RNFR":
				    	if (checkAuthentication(UserProfile.ADMINISTRADOR))
				    		serverFunctions.handleRenameFromCommand(commandArg);
				        break;

				    case "RNTO":
				    	if (checkAuthentication(UserProfile.ADMINISTRADOR))
				    		serverFunctions.handleRenameToCommand(commandArg);
				        break;
				        
				    case "CWD":
				    	if (checkAuthentication(UserProfile.BASICO, UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR))
				    		serverFunctions.handleChangeWorkingDirectory(commandArg);
				        break;

				    case "CDUP":
				    	if (checkAuthentication(UserProfile.BASICO, UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR))
				    		serverFunctions.handleChangeToParentDirectory();
				        break;

				    case "PWD":
				    	if (checkAuthentication(UserProfile.BASICO, UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR))
				    		serverFunctions.handlePrintWorkingDirectory();
				        break;

				    case "QUIT":
				    	sendReply(221, "Goodbye.");
				    	Util.printGreenColor("Cliente desconectado mediante comando QUIT");
				    	return; // Salir del bucle y cerrar conexión

					default:
						Util.printRedColor("Comando desconocido recibido: " + command);
						sendReply(502, "Comando no implementado");
						break;
				}
			}

		} catch (IOException e) {
			Util.printRedColor("\nError con el cliente: " + e.getMessage());
		} catch (Exception e) {
			Util.printRedColor("\nError: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (in != null) in.close();
				if (out != null) out.close();
				controlSocket.close();
			} catch (IOException e) {
				Util.printRedColor("\nError al cerrar la conexión del cliente: " + e.getMessage());
			}
		}
	}

	/**
	 * Obtiene el directorio de trabajo actual del cliente.
	 *
	 * @return Ruta del directorio actual
	 */
	public String getCurrentDirectory() {
	    return currentDirectory;
	}

	/**
	 * Establece el directorio de trabajo actual del cliente.
	 *
	 * @param currentDirectory Nueva ruta del directorio
	 */
	public void setCurrentDirectory(String currentDirectory) {
	    this.currentDirectory = currentDirectory;
	}

	/**
	 * Maneja el comando USER para iniciar la autenticación.
	 *
	 * @param usernameInput Nombre de usuario proporcionado
	 */
	private void handleUserCommand(String usernameInput) {
	    if (userExists(usernameInput)) {
	    	usernameBuffer = usernameInput; // Guardamos el usuario temporalmente
	        sendReply(331, "Username okay, need password.");
	    } else {
	        sendReply(530, "Authentication failed.");
	    }
	}

	/**
	 * Maneja el comando PASS para completar la autenticación.
	 *
	 * @param passwordInput Contraseña proporcionada
	 */
	private void handlePassCommand(String passwordInput) {
	    if (usernameBuffer == null) {
	        sendReply(530, "Authentication failed."); // No se ha enviado USER antes
	        return;
	    }

	    if (authenticateUser(usernameBuffer, passwordInput)) {
	        sendReply(230, "User logged in, proceed.");
	        Util.printGreenColor("Usuario logeado correctamente");
	        FTP.Util.FileLogger.logAuth(usernameBuffer, true);
	    } else {
	        sendReply(530, "Authentication failed.");
	        FTP.Util.FileLogger.logAuth(usernameBuffer, false);
	    }

	    usernameBuffer = null; // Resetear el estado después de PASS
	}

	/**
	 * Maneja el comando TYPE para establecer el tipo de transferencia.
	 *
	 * @param type Tipo de transferencia ('A' para ASCII, 'I' para binario)
	 */
	private void handleTypeCommand(String type) {
	    if (type == null || type.isEmpty()) {
	        sendReply(501, "Syntax error in parameters or arguments.");
	        return;
	    }

	    if (type.equalsIgnoreCase("A")) {
	        transferType = "A";
	        sendReply(200, "Type set to ASCII.");
	    } else if (type.equalsIgnoreCase("I")) {
	        transferType = "I";
	        sendReply(200, "Type set to Binary.");
	    } else {
	        sendReply(504, "Command not implemented for that parameter.");
	    }
	}

	/**
	 * Maneja el comando PASV para configurar modo pasivo.
	 * El servidor abre un puerto y espera que el cliente se conecte.
	 */
	private void handlePasvCommand() {
		int p1, p2;
		
		connectionMode = "PASSIVE";

		try {
			passiveDataSocket = new ServerSocket(0);
			passiveDataPort = passiveDataSocket.getLocalPort();
			
			passiveDataIp = controlSocket.getInetAddress().getHostAddress();

		    // Envía la respuesta PASV al cliente
		    p1 = passiveDataPort / 256;
		    p2 = passiveDataPort % 256;
		    sendReply(227, "Entering Passive Mode (" + passiveDataIp.replace(".", ",") + "," + p1 + "," + p2 + ")");
		    
		    System.out.println("Modo pasivo configurado en IP " + passiveDataIp + " y puerto " + passiveDataPort);
		} catch (IOException e) {
			sendReply(425, "Can't open data connection");
		}
	}

	/**
	 * Maneja el comando PORT para configurar modo activo.
	 * El cliente proporciona su dirección IP y puerto para la conexión de datos.
	 *
	 * @param address Cadena con formato "h1,h2,h3,h4,p1,p2" donde h es IP y p es puerto
	 */
	private void handlePortCommand(String address) {
		String[] addressParts;
		
	    if (address == null) {
	        sendReply(501, "Syntax error in parameters or arguments.");
	        return;
	    }
		
		connectionMode = "ACTIVE";
		addressParts = address.split(","); 
		
		activeDataIp = addressParts[0] + "." + addressParts[1] + "." + addressParts[2] + "." + addressParts[3];
		activeDataPort = Integer.parseInt(addressParts[4]) * 256 + Integer.parseInt(addressParts[5]);
		
		sendReply(200, "PORT command successful");
		
		System.out.println("Modo activo configurado en IP " + activeDataIp + " y puerto " + activeDataPort);
	}

	/**
	 * Obtiene el socket de datos según el modo de conexión configurado.
	 * En modo PASSIVE acepta la conexión del cliente, en ACTIVE se conecta al cliente.
	 *
	 * @return Socket de datos establecido
	 * @throws IOException Si no se puede establecer la conexión de datos
	 */
    protected Socket getDataSocket() throws IOException {
		synchronized (this) { // Evita condiciones de carrera si varios hilos acceden a la conexión
	        if (connectionMode.equals("PASSIVE")) {
	            if (passiveDataSocket == null) {
	                throw new IOException("Modo pasivo no inicializado.");
	            }

	            passiveDataSocket.setSoTimeout(10000); // Timeout de 10 segundos para evitar bloqueos
	            
	            return passiveDataSocket.accept(); // Cliente se conecta en PASV
	            
	        } else if (connectionMode.equals("ACTIVE")) {
	            if (activeDataIp == null || activeDataPort == 0) {
	                throw new IOException("Modo activo no inicializado.");
	            }
	        	
	            activeDataSocket = new Socket(activeDataIp, activeDataPort);
	            return activeDataSocket; // Cliente se conecta en PORT
	            
	        } else {
				throw new IOException("Modo de conexión no válido");
			}
		}
    }

	/**
	 * Cierra el socket de datos y libera recursos.
	 * Maneja tanto modo ACTIVE como PASSIVE de forma segura.
	 */
    protected void closeDataSocket() {
        synchronized (this) {  // Evita problemas si varios hilos acceden al cierre de sockets
            try {
                if (activeDataSocket != null) {
                    activeDataSocket.close();
                    activeDataSocket = null;
                }

                if (passiveDataSocket != null && "PASSIVE".equals(connectionMode)) {
                    passiveDataSocket.close();
                    passiveDataSocket = null;
                }
            } catch (IOException e) {
            	Util.printRedColor("Error al cerrar socket de datos: " + e.getMessage());
            }
        }
    }

	/**
	 * Verifica si un nombre de usuario existe en el archivo de usuarios.
	 *
	 * @param usernameInput Nombre de usuario a verificar
	 * @return true si el usuario existe, false en caso contrario
	 */
	private boolean userExists(String usernameInput) {
		try (FileInputStream inputStream = new FileInputStream(new File(JavaFtpServer.USERS_FILE));
				InputStreamReader reader = new InputStreamReader(inputStream);
				BufferedReader buffer = new BufferedReader(reader)) {
	    	
	        String line;
	        while ((line = buffer.readLine()) != null) {
	            String[] credentials = line.split(":");
	            if (credentials[0].equals(usernameInput)) {
	                return true;
	            }
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		
	    return false;
	}

	/**
	 * Autentica un usuario verificando nombre de usuario y contraseña.
	 * Si la autenticación es exitosa, establece el usuario actual con su perfil.
	 *
	 * @param usernameInput Nombre de usuario
	 * @param passwordInput Contraseña
	 * @return true si la autenticación es exitosa, false en caso contrario
	 */
	private boolean authenticateUser(String usernameInput, String passwordInput) {
		try (FileInputStream inputStream = new FileInputStream(new File(JavaFtpServer.USERS_FILE));
				InputStreamReader reader = new InputStreamReader(inputStream);
				BufferedReader buffer = new BufferedReader(reader)) {

			String line;
			while ((line = buffer.readLine()) != null) {
				String[] credentials = line.split(":");
				if (credentials[0].equals(usernameInput) && credentials[1].equals(passwordInput)) {
					currentUser = new User(credentials[0], credentials[1], UserProfile.valueOf(credentials[2]));
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Envía una respuesta FTP al cliente.
	 *
	 * @param code Código de respuesta FTP (ej: 200, 530, etc.)
	 * @param message Mensaje descriptivo de la respuesta
	 */
	protected void sendReply(int code, String message) {
		out.printf("%d %s%n", code, message);
	}

	/**
	 * Verifica si el usuario actual tiene autorización para ejecutar un comando.
	 * Comprueba si el perfil del usuario coincide con alguno de los perfiles permitidos.
	 *
	 * @param allowedProfiles Perfiles que tienen permiso para el comando
	 * @return true si el usuario está autenticado y tiene el perfil adecuado, false en caso contrario
	 */
	private boolean checkAuthentication(UserProfile... allowedProfiles) {
		if (currentUser == null || currentUser.getProfile() == null) {
			Util.printRedColor("Usuario no autenticado");
		    sendReply(530, "530 Not logged in");
		    return false;
		}
		
	    for (UserProfile allowed : allowedProfiles) {
	        if (currentUser.getProfile().equals(allowed)) {
	            return true;
	        }
	    }
	    
	    sendReply(550, "Permission denied.");
	    Util.printRedColor("Permiso denegado, el usuario no tiene permisos suficientes");
	    return false;
	}
}