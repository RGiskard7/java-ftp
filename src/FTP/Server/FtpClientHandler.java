package FTP.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.mindrot.jbcrypt.BCrypt;

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
	private final ServerFunctions serverFunctions;

	/** Directorio de trabajo actual del cliente */
	private String currentDirectory;

	/** Tipo de transferencia actual: "A" (ASCII) o "I" (Binary/Image) */
	private String transferType = "A";

	/** Configuración del servidor (timeout, rango pasivo, etc.) */
	private final ServerConfig config;

	/** Throttle de intentos de login por IP */
	private final LoginThrottle loginThrottle;

	/** Almacén de usuarios (SQLite o fichero) */
	private final UserStore userStore;

	/** Timestamp del último comando (para timeout de sesión; usado en log al cerrar por timeout) */
	private long lastActivityAt;

	/** Si el canal de control está cifrado (AUTH TLS) */
	private boolean tlsActive;

	/** Si el canal de datos debe cifrarse (PROT P) */
	private boolean dataProtection;

	/**
	 * Constructor del manejador de cliente.
	 *
	 * @param controlSocket Socket de control establecido con el cliente
	 * @param config Configuración del servidor (puede ser null para valores por defecto)
	 * @param loginThrottle Throttle de login (puede ser null para desactivar)
	 * @param userStore Almacén de usuarios (SQLite o fichero)
	 * @throws IOException Si ocurre un error al inicializar el manejador
	 */
	public FtpClientHandler(Socket controlSocket, ServerConfig config, LoginThrottle loginThrottle, UserStore userStore) throws IOException {
		this.controlSocket = controlSocket;
		this.config = config != null ? config : new ServerConfig();
		this.loginThrottle = loginThrottle;
		this.userStore = userStore != null ? userStore : new FileUserStore(JavaFtpServer.USERS_FILE);
		currentUser = new User();
		serverFunctions = new ServerFunctions(this);
		currentDirectory = JavaFtpServer.dirRoot;
		this.lastActivityAt = System.currentTimeMillis();
		this.tlsActive = false;
		this.dataProtection = false;
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
			long sessionTimeout = config.getSessionTimeout();
			if (sessionTimeout > 0) {
				int soTimeout = (int) Math.min(sessionTimeout, Integer.MAX_VALUE);
				controlSocket.setSoTimeout(soTimeout);
			}

			// Usar UTF-8 explícitamente para soportar nombres con acentos, ñ, etc.
			in = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), "UTF-8"));
			out = new PrintWriter(controlSocket.getOutputStream(), true);

			sendReply(220, "Welcome to the FTP server");

			while ((command = in.readLine()) != null) {
				lastActivityAt = System.currentTimeMillis();
				String[] commandParts = command.split(" ", 2); // Límite 2: comando + resto
			    String commandName = commandParts[0];
			    String commandArg = (commandParts.length > 1) ? commandParts[1] : null; // Toma TODO después del espacio

				if (config.isTlsRequired() && !tlsActive && !commandName.equals("AUTH") && !commandName.equals("QUIT")) {
					sendReply(530, "Please use AUTH TLS first.");
					continue;
				}

				System.out.println("\nModo conexión: " +  ((connectionMode != null) ? connectionMode : "NO ESPECIFICADO"));
				System.out.println("Tipo usuario: " + currentUser.getProfile());
				System.out.println("Comando recibido: " + command);
				
				switch (commandName) {
					case "AUTH":
						handleAuthCommand(commandArg);
						break;

					case "PROT":
						handleProtCommand(commandArg);
						break;

			        case "SYST":
			            sendReply(215, "UNIX Type: L8");
			            break;

					case "FEAT":
						sendReplyMultilineStart(211, "Extensions supported:");
						sendReplyMultilineBody(" UTF8");
						sendReplyMultilineBody(" SIZE");
						sendReplyMultilineBody(" MDTM");
						sendReplyMultilineBody(" TVFS");
						sendReply(211, "End");
						break;

					case "OPTS":
						if (commandArg != null && commandArg.trim().toUpperCase().startsWith("UTF8 ON")) {
							sendReply(200, "OPTS UTF8 OK");
						} else if (commandArg != null && commandArg.trim().toUpperCase().startsWith("UTF8 OFF")) {
							sendReply(504, "UTF8 mode is always on.");
						} else {
							sendReply(501, "Syntax error in parameters or arguments.");
						}
						break;

					case "NOOP":
						sendReply(200, "OK");
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

					case "SIZE":
						if (checkAuthentication(UserProfile.BASICO, UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR))
							handleSizeCommand(commandArg);
						break;

					case "MDTM":
						if (checkAuthentication(UserProfile.BASICO, UserProfile.INTERMEDIO, UserProfile.ADMINISTRADOR))
							handleMdtmCommand(commandArg);
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

		} catch (SocketTimeoutException e) {
			Util.printYellowColor("\nSesión cerrada por timeout de inactividad");
			FTP.Util.FileLogger.info("Cliente desconectado por timeout: " + controlSocket.getInetAddress() + " (última actividad: " + lastActivityAt + ")");
		} catch (IOException e) {
			Util.printRedColor("\nError con el cliente: " + e.getMessage());
		} catch (Exception e) {
			Util.printRedColor("\nError: " + e.getMessage());
			FTP.Util.FileLogger.error("Error en handler: " + e.getMessage());
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

	/** Nombre del usuario actual (para auditoría) o null si no autenticado */
	public String getCurrentUsername() {
	    return currentUser != null && currentUser.getProfile() != null ? currentUser.getUsername() : "-";
	}

	/** Dirección IP del cliente (para auditoría) */
	public String getClientAddress() {
	    return controlSocket != null ? controlSocket.getInetAddress().getHostAddress() : "-";
	}

	/** Tipo de transferencia actual (A=ASCII, I=Binary) según comando TYPE */
	public String getTransferType() {
	    return transferType;
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
	    usernameBuffer = usernameInput;
	    sendReply(331, "Username okay, need password.");
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

	    String clientIp = controlSocket.getInetAddress().getHostAddress();
	    if (loginThrottle != null && loginThrottle.isBlocked(clientIp)) {
	        sendReply(530, "Too many failed attempts. Try again later.");
	        FTP.Util.FileLogger.logAuthBlocked(clientIp);
	        usernameBuffer = null;
	        return;
	    }

	    UserInfo info = userStore.findByUsername(usernameBuffer);
	    if (info != null && info.isEnabled() && BCrypt.checkpw(passwordInput, info.getPasswordHash())) {
	        if (loginThrottle != null) loginThrottle.clear(clientIp);
	        currentUser = new User(info.getUsername(), info.getProfile());
	        sendReply(230, "User logged in, proceed.");
	        Util.printGreenColor("Usuario logeado correctamente");
	        FTP.Util.FileLogger.logAuth(usernameBuffer, true);
	    } else {
	        if (loginThrottle != null) loginThrottle.recordFailure(clientIp);
	        sendReply(530, "Authentication failed.");
	        FTP.Util.FileLogger.logAuth(usernameBuffer, false);
	    }

	    usernameBuffer = null;
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
	 * Maneja AUTH TLS: envuelve el canal de control en TLS.
	 */
	private void handleAuthCommand(String arg) {
		if (arg == null || !arg.equalsIgnoreCase("TLS")) {
			sendReply(504, "AUTH not supported for that parameter.");
			return;
		}
		SSLContext ctx = config.getSslContext();
		if (ctx == null) {
			sendReply(502, "TLS not configured.");
			return;
		}
		// RFC 4217: send 234 over plaintext BEFORE the TLS handshake
		sendReply(234, "Proceed with negotiation.");
		out.flush();

		SSLSocket sslSocket = null;
		try {
			sslSocket = (SSLSocket) ctx.getSocketFactory().createSocket(
				controlSocket,
				controlSocket.getInetAddress().getHostAddress(),
				controlSocket.getPort(),
				true
			);
			sslSocket.setUseClientMode(false);
			sslSocket.startHandshake();
			in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), "UTF-8"));
			out = new PrintWriter(sslSocket.getOutputStream(), true);
			controlSocket = sslSocket;
			sslSocket = null;
			tlsActive = true;
		} catch (Exception e) {
			FTP.Util.FileLogger.error("AUTH TLS fallido: " + e.getMessage());
			Util.printRedColor("AUTH TLS fallido: " + e.getMessage());
			if (sslSocket != null) {
				try { sslSocket.close(); } catch (IOException ignored) { }
			}
		}
	}

	/**
	 * Maneja PROT P (datos cifrados) y PROT C (datos en claro).
	 */
	private void handleProtCommand(String arg) {
		if (arg == null) {
			sendReply(501, "Syntax error in parameters or arguments.");
			return;
		}
		if (arg.equalsIgnoreCase("P")) {
			dataProtection = true;
			sendReply(200, "PROT P OK.");
		} else if (arg.equalsIgnoreCase("C")) {
			dataProtection = false;
			sendReply(200, "PROT C OK.");
		} else {
			sendReply(504, "PROT not supported for that parameter.");
		}
	}

	/**
	 * Maneja el comando PASV para configurar modo pasivo.
	 * El servidor abre un puerto y espera que el cliente se conecte.
	 */
	private void handlePasvCommand() {
		int p1, p2;
		
		connectionMode = "PASSIVE";

		if (passiveDataSocket != null) {
			try { passiveDataSocket.close(); } catch (IOException ignored) { }
			passiveDataSocket = null;
		}

		try {
			int min = config.getPassivePortMin();
			int max = config.getPassivePortMax();
			if (min > 0 && max >= min) {
				Random r = new Random();
				int rangeSize = max - min + 1;
				int tries = Math.min(rangeSize, 100);
				for (int i = 0; i < tries; i++) {
					int port = min + r.nextInt(rangeSize);
					try {
						passiveDataSocket = new ServerSocket(port);
						passiveDataPort = port;
						break;
					} catch (IOException ignored) {
						// puerto en uso, intentar otro
					}
				}
			}
			if (passiveDataSocket == null) {
				passiveDataSocket = new ServerSocket(0);
				passiveDataPort = passiveDataSocket.getLocalPort();
			}
			
			passiveDataIp = controlSocket.getLocalAddress().getHostAddress();

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
	 * Valida que la IP coincida con la del cliente (anti-SSRF) y que el puerto esté en rango permitido.
	 *
	 * @param address Cadena con formato "h1,h2,h3,h4,p1,p2" donde h es IP y p es puerto
	 */
	private void handlePortCommand(String address) {
		if (address == null || address.isEmpty()) {
			sendReply(501, "Syntax error in parameters or arguments.");
			return;
		}
		String[] addressParts = address.split(",");
		if (addressParts.length != 6) {
			sendReply(501, "Syntax error in parameters or arguments.");
			return;
		}
		try {
			activeDataIp = addressParts[0].trim() + "." + addressParts[1].trim() + "." + addressParts[2].trim() + "." + addressParts[3].trim();
			int p1 = Integer.parseInt(addressParts[4].trim());
			int p2 = Integer.parseInt(addressParts[5].trim());
			activeDataPort = p1 * 256 + p2;
			if (activeDataPort < 1024 || activeDataPort > 65535) {
				sendReply(501, "Port number must be between 1024 and 65535.");
				return;
			}
			String clientIp = controlSocket.getInetAddress().getHostAddress();
			if (!clientIp.equals(activeDataIp)) {
				sendReply(503, "PORT IP must match your connection address.");
				Util.printRedColor("PORT rechazado: IP " + activeDataIp + " no coincide con cliente " + clientIp);
				return;
			}
		} catch (NumberFormatException e) {
			sendReply(501, "Syntax error in parameters or arguments.");
			return;
		}
		connectionMode = "ACTIVE";
		sendReply(200, "PORT command successful");
		System.out.println("Modo activo configurado en IP " + activeDataIp + " y puerto " + activeDataPort);
	}

	/**
	 * Maneja el comando SIZE: devuelve el tamaño en bytes del archivo.
	 * Requiere autenticación. Resuelve la ruta igual que LIST/RETR.
	 */
	private void handleSizeCommand(String pathArg) {
		if (pathArg == null || pathArg.trim().isEmpty()) {
			sendReply(501, "Syntax error in parameters or arguments.");
			return;
		}
		File file = serverFunctions.resolvePathToFile(pathArg.trim());
		if (file == null || !file.exists()) {
			sendReply(550, "File not found or access denied.");
			return;
		}
		if (!file.isFile()) {
			sendReply(550, "Not a plain file.");
			return;
		}
		sendReply(213, String.valueOf(file.length()));
	}

	/**
	 * Maneja el comando MDTM: devuelve la fecha de modificación del archivo en formato YYYYMMDDHHmmss.
	 * Requiere autenticación. Resuelve la ruta igual que LIST/RETR.
	 */
	private void handleMdtmCommand(String pathArg) {
		if (pathArg == null || pathArg.trim().isEmpty()) {
			sendReply(501, "Syntax error in parameters or arguments.");
			return;
		}
		File file = serverFunctions.resolvePathToFile(pathArg.trim());
		if (file == null || !file.exists()) {
			sendReply(550, "File not found or access denied.");
			return;
		}
		if (!file.isFile()) {
			sendReply(550, "Not a plain file.");
			return;
		}
		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
		String mdtm = fmt.format(new java.util.Date(file.lastModified()));
		sendReply(213, mdtm);
	}

	/**
	 * Obtiene el socket de datos según el modo de conexión configurado.
	 * En modo PASSIVE acepta la conexión del cliente, en ACTIVE se conecta al cliente.
	 *
	 * @return Socket de datos establecido
	 * @throws IOException Si no se puede establecer la conexión de datos
	 */
    protected Socket getDataSocket() throws IOException {
		Socket dataSocket;
		synchronized (this) {
	        if (connectionMode.equals("PASSIVE")) {
	            if (passiveDataSocket == null) {
	                throw new IOException("Modo pasivo no inicializado.");
	            }
	            passiveDataSocket.setSoTimeout(10000); // Timeout de 10 segundos para evitar bloqueos
	            dataSocket = passiveDataSocket.accept();
	        } else if (connectionMode.equals("ACTIVE")) {
	            if (activeDataIp == null || activeDataPort == 0) {
	                throw new IOException("Modo activo no inicializado.");
	            }
	            activeDataSocket = new Socket(activeDataIp, activeDataPort);
	            dataSocket = activeDataSocket;
	        } else {
				throw new IOException("Modo de conexión no válido");
			}
		}
		if (dataProtection && config.getSslContext() != null) {
			SSLSocket sslData = (SSLSocket) config.getSslContext().getSocketFactory().createSocket(
				dataSocket,
				dataSocket.getInetAddress().getHostName(),
				dataSocket.getPort(),
				true
			);
			sslData.startHandshake();
			return sslData;
		}
		return dataSocket;
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
	 * Envía una respuesta FTP al cliente.
	 *
	 * @param code Código de respuesta FTP (ej: 200, 530, etc.)
	 * @param message Mensaje descriptivo de la respuesta
	 */
	protected void sendReply(int code, String message) {
		out.print(code + " " + message + "\r\n");
		out.flush();
	}

	/** Envía la primera línea de una respuesta multilínea (código con guión, ej. "211-Extensions"). */
	private void sendReplyMultilineStart(int code, String line) {
		out.print(code + "-" + line + "\r\n");
		out.flush();
	}

	/** Envía una línea del cuerpo de una respuesta multilínea (sin código, solo texto). */
	private void sendReplyMultilineBody(String line) {
		out.print(line + "\r\n");
		out.flush();
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
		    sendReply(530, "Not logged in.");
		    return false;
		}
		
	    for (UserProfile allowed : allowedProfiles) {
	        if (currentUser.getProfile().equals(allowed)) {
	            return true;
	        }
	    }
	    
	    sendReply(550, "Permission denied.");
	    FTP.Util.FileLogger.logAudit(getCurrentUsername(), getClientAddress(), "PERM_DENIED", command != null ? command : "");
	    Util.printRedColor("Permiso denegado, el usuario no tiene permisos suficientes");
	    return false;
	}
}