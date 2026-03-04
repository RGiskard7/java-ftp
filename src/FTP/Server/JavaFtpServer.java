package FTP.Server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import FTP.Util.Util;

/**
 * Servidor FTP principal que maneja conexiones concurrentes de clientes.
 * <p>
 * Este servidor implementa el protocolo FTP con soporte para:
 * <ul>
 *   <li>Autenticación de usuarios basada en archivo</li>
 *   <li>Control de acceso basado en roles (RBAC)</li>
 *   <li>Modos de transferencia ACTIVE y PASSIVE</li>
 *   <li>Manejo concurrente de múltiples clientes mediante ExecutorService</li>
 * </ul>
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class JavaFtpServer {
	/** Directorio raíz desde donde el servidor sirve archivos */
	protected static String dirRoot;

	/** Ruta al archivo de usuarios (modo legacy; puede sobreescribirse desde config) */
	protected static String usersFilePath;

	/** Almacén de usuarios (SQLite o fichero); compartido por todos los handlers */
	protected static UserStore userStore;

	/** Directorio base para archivos del sistema */
	protected static final String FILES_DIR = "files";

	/** Directorio que contiene información de usuarios */
	protected static final String USERS_DIR = FILES_DIR + File.separator + "users";

	/** Archivo que almacena las credenciales de usuario (formato: username:bcryptHash:profile) */
	protected static final String USERS_FILE = USERS_DIR + File.separator + "users.txt";

	/** Puerto de control FTP estándar */
	protected static final int CONTROL_PORT = 21;

	/** Flag para cierre ordenado (graceful shutdown) */
	private static volatile boolean shuttingDown = false;

	/** Referencia al ServerSocket para que el shutdown hook pueda cerrarlo */
	private static volatile ServerSocket serverRef = null;

	/**
	 * Verifica la existencia de directorios y archivos necesarios para el servidor.
	 * Valida que existan la carpeta 'files' y el archivo 'users.txt'.
	 */
    private static void checkDirs() {
		File dir = new File(FILES_DIR);
		if (!dir.exists() || !dir.isDirectory()) {
			Util.printRedColor("\nERROR CRÍTICO: La carpeta 'files' no existe en la raiz del programa");
			return;
		}
		
		File file = new File(USERS_FILE);
		if (!file.exists() || !file.isFile()) {
			Util.printRedColor("\nERROR CRÍTICO: El fichero 'users.txt' no se encuentra en la carpeta 'file'");
		}
    }

	/**
	 * Solicita y configura el directorio raíz del servidor FTP.
	 * Valida que el directorio exista antes de aceptarlo.
	 *
	 * @param sc Scanner para leer entrada del usuario
	 */
    private static void setRoot(Scanner sc) {
    	String input;
    	File file;
    	
        do {        	
        	do {
            	System.out.println("\nIntroduzca el directorio raiz del servidor FTP");
            	System.out.print(":> ");
        		
        		input = sc.nextLine();
        		if (input == null || input.trim().isEmpty()) {
        			Util.printRedColor("\nLa dirección root del servidor no puede estar vacía");
        			input = null;	
        		}
        	} while (input == null);

        	dirRoot = input;
        	file = new File(dirRoot);
        	if (!file.exists()) 
        		Util.printRedColor("\nLa dirección root proporcionada para el servidor no existe, pruebe de nuevo.");	
        	
        } while (!file.exists());
    }

	/**
	 * Punto de entrada principal del servidor FTP.
	 * Inicializa el servidor, muestra el banner, configura el directorio raíz
	 * y comienza a escuchar conexiones de clientes en el puerto 21.
	 *
	 * @param args Argumentos de línea de comandos (no utilizados)
	 * @throws InterruptedException Si el servidor es interrumpido
	 */
    public static void main(String[] args) throws InterruptedException {
        ExecutorService execute = null;
        Scanner sc = null;
        ServerConfig config = new ServerConfig();
        int serverPort;

        System.out.println(" _____                 _     _              _____ _____ _____");
        System.out.println("/  ___|               (_)   | |            |  ___|_   _| ___ \\");
        System.out.println("\\ `--.  ___ _ ____   ___  __| | ___  _ __  | |_    | | | |_/ /");
        System.out.println(" `--. \\/ _ \\ '__\\ \\ / / |/ _` |/ _ \\| '__| |  _|   | | |  __/ ");
        System.out.println("/\\__/ /  __/ |   \\ V /| | (_| | (_) | |    | |     | | | |    ");
        System.out.println("\\____/ \\___|_|    \\_/ |_|\\__,_|\\___/|_|    \\_|     \\_/ \\_|    ");
        System.out.println("\nBy Eduardo Díaz");

        FTP.Util.FileLogger.initialize();
        FTP.Util.FileLogger.info("========== SERVIDOR FTP INICIADO ==========");

        usersFilePath = USERS_FILE;

        try {
            config.loadFromFile("server.properties");
            Util.printGreenColor("\n✓ Configuración cargada desde server.properties");
            serverPort = config.getControlPort();
            usersFilePath = config.getUsersFile();
            FTP.Util.FileLogger.setVerbose(config.isVerboseLogging());
            FTP.Util.FileLogger.setRotation(config.getLogMaxSizeBytes(), config.getLogMaxBackupFiles());

            if (!config.getRootDirectory().isEmpty()) {
                dirRoot = config.getRootDirectory();
                File rootFile = new File(dirRoot);
                if (!rootFile.exists() || !rootFile.isDirectory()) {
                    Util.printRedColor("\nERROR: Directorio raíz no existe: " + dirRoot);
                    FTP.Util.FileLogger.error("Directorio raíz no existe: " + dirRoot);
                    return;
                }
                String dbPath = config.getUsersDatabase();
                if (dbPath != null && !dbPath.isEmpty()) {
                    File dbFile = new File(dbPath);
                    try {
                        if (!dbFile.exists()) {
                            File parent = dbFile.getParentFile();
                            if (parent != null) parent.mkdirs();
                        }
                        SqliteUserStore sqliteStore = new SqliteUserStore(dbPath);
                        sqliteStore.initSchema();
                        userStore = sqliteStore;
                        Util.printGreenColor("✓ Usuarios: SQLite (" + dbPath + ")");
                    } catch (SQLException e) {
                        Util.printRedColor("\nERROR: No se pudo inicializar la base de usuarios: " + e.getMessage());
                        FTP.Util.FileLogger.error("SQLite usuarios: " + e.getMessage());
                        return;
                    }
                } else {
                    usersFilePath = config.getUsersFile();
                    File usersFile = new File(usersFilePath);
                    if (!usersFile.exists() || !usersFile.isFile()) {
                        Util.printRedColor("\nERROR: Archivo de usuarios no existe: " + usersFilePath);
                        FTP.Util.FileLogger.error("Archivo de usuarios no existe: " + usersFilePath);
                        return;
                    }
                    userStore = new FileUserStore(usersFilePath);
                    Util.printGreenColor("✓ Usuarios: fichero (" + usersFilePath + ")");
                }
                Util.printGreenColor("✓ Directorio raíz: " + dirRoot);
            } else {
                checkDirs();
                sc = new Scanner(System.in);
                setRoot(sc);
            }
        } catch (IOException e) {
            Util.printRedColor("\n⚠ No se pudo cargar server.properties, usando configuración por defecto");
            serverPort = CONTROL_PORT;
            checkDirs();
            sc = new Scanner(System.in);
            setRoot(sc);
            usersFilePath = config.getUsersFile();
            userStore = new FileUserStore(usersFilePath);
        }

        if (userStore == null) {
            usersFilePath = config.getUsersFile();
            userStore = new FileUserStore(usersFilePath);
        }

        int maxConn = config.getMaxConnections();
        execute = Executors.newFixedThreadPool(maxConn);
        Semaphore connectionLimit = new Semaphore(maxConn);
        LoginThrottle loginThrottle = new LoginThrottle(config.getAuthMaxAttempts(), config.getAuthLockoutMinutes());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shuttingDown = true;
            try {
                if (serverRef != null) serverRef.close();
            } catch (IOException ignored) { }
        }));

        ServerSocket server = null;
        try {
            server = new ServerSocket(serverPort);
            serverRef = server;
            Util.printGreenColor("\nServidor FTP iniciado en el puerto de control " + serverPort);
            FTP.Util.FileLogger.info("Servidor escuchando en puerto " + serverPort + ", max conexiones: " + maxConn);

            while (!shuttingDown) {
                try {
                    Socket client = server.accept();
                    if (!connectionLimit.tryAcquire()) {
                        try {
                            client.getOutputStream().write("421 Too many connections. Try again later.\r\n".getBytes());
                            client.close();
                        } catch (IOException ignored) { }
                        FTP.Util.FileLogger.warning("Conexión rechazada: límite alcanzado desde " + client.getInetAddress());
                        continue;
                    }
                    FTP.Util.FileLogger.logConnection(client.getInetAddress().toString());
                    final Semaphore permit = connectionLimit;
                    final UserStore store = userStore;
                    execute.execute(() -> {
                        try {
                            FtpClientHandler handler = new FtpClientHandler(client, config, loginThrottle, store);
                            handler.run();
                        } catch (IOException e) {
                            Util.printRedColor("Error iniciando handler: " + e.getMessage());
                        } finally {
                            permit.release();
                        }
                    });
                } catch (SocketException e) {
                    if (shuttingDown) break;
                    throw e;
                }
            }
        } catch (IOException e) {
            if (!shuttingDown) {
                Util.printRedColor("\nError en el servidor: " + e.getMessage());
                FTP.Util.FileLogger.error("Error en servidor: " + e.getMessage());
            }
        } catch (Exception e) {
            Util.printRedColor("\nError: " + e.getMessage());
            FTP.Util.FileLogger.error("Error inesperado: " + e.getMessage());
        } finally {
            serverRef = null;
            if (server != null) try { server.close(); } catch (IOException ignored) { }
            if (execute != null) {
                execute.shutdown();
                try {
                    if (!execute.awaitTermination(10, TimeUnit.SECONDS))
                        execute.shutdownNow();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    execute.shutdownNow();
                }
            }
            if (sc != null) sc.close();
        }
        FTP.Util.FileLogger.info("========== SERVIDOR FTP DETENIDO ==========");
    }
}