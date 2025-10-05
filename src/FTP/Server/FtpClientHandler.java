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
 * @author Eduardo Díaz Sánchez
 */
public class FtpClientHandler implements Runnable {
	private Socket controlSocket;
	private String connectionMode;
	
	// Para el modo pasivo
	private ServerSocket passiveDataSocket;
	private String passiveDataIp;
	private Integer passiveDataPort;

	// Para el modo pasivo
	private Socket activeDataSocket;
	private String activeDataIp;
	private Integer activeDataPort;
	
	// Flujos de comunicación
	private BufferedReader in;
	private PrintWriter out;
	
	private String usernameBuffer = null;
	private User currentUser;
	private String command;
	
	private ServerFunctions serverFunctions;
	private String currentDirectory;

	public FtpClientHandler(Socket controlSocket) throws IOException {
		this.controlSocket = controlSocket;
		currentUser = new User();
		serverFunctions = new ServerFunctions(this);
		currentDirectory = JavaFtpServer.dirRoot;
	}
	
	@Override
	public void run() {
		System.out.println("\n[SOLICITUD RECIBIDA]");
		System.out.println("\nConexión con el cliente " + controlSocket.getInetAddress());

		try {
			in = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
			out = new PrintWriter(controlSocket.getOutputStream(), true);

			sendReply(220, "Welcome to the FTP server");

			while ((command = in.readLine()) != null) {
				System.out.println("\nModo conexión: " +  ((connectionMode != null) ? connectionMode : "NO ESPECIFICADO"));
				System.out.println("Tipo usuario: " + currentUser.getProfile());
				System.out.println("Comando recibido: " + command);
				
				String[] commandParts = command.split(" ");
			    String commandName = commandParts[0];
			    String commandArg = (commandParts.length > 1) ? commandParts[1] : null; // Evitar error si no hay argumento
				
				switch (commandName) {
			        case "SYST":
			            sendReply(215, "UNIX Type: L8");
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
				    	if (checkAuthentication(UserProfile.ADMINISTRADOR))
				    		serverFunctions.handleChangeWorkingDirectory(commandArg);
				        break;
				        
				    case "PWD":
				    	if (checkAuthentication(UserProfile.BASICO, UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR))
				    		serverFunctions.handleChangeToParentDirectory();
				        break;

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
	
	public String getCurrentDirectory() {
	    return currentDirectory;
	}

	public void setCurrentDirectory(String currentDirectory) {
	    this.currentDirectory = currentDirectory;
	}

	private void handleUserCommand(String usernameInput) {
	    if (userExists(usernameInput)) {
	    	usernameBuffer = usernameInput; // Guardamos el usuario temporalmente
	        sendReply(331, "Username okay, need password.");
	    } else {
	        sendReply(530, "Authentication failed.");
	    }
	}
	
	private void handlePassCommand(String passwordInput) {
	    if (usernameBuffer == null) {
	        sendReply(530, "Authentication failed."); // No se ha enviado USER antes
	        return;
	    }

	    if (authenticateUser(usernameBuffer, passwordInput)) {
	        sendReply(230, "User logged in, proceed.");
	        Util.printGreenColor("Usuario logeado correctamente");
	    } else {
	        sendReply(530, "Authentication failed.");
	    }

	    usernameBuffer = null; // Resetear el estado después de PASS
	}
	
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
	


	protected void sendReply(int code, String message) {
		out.printf("%d %s%n", code, message);
	}
	
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