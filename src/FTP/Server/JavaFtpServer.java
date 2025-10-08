package FTP.Server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

	/** Directorio base para archivos del sistema */
	protected static final String FILES_DIR = "files";

	/** Directorio que contiene información de usuarios */
	protected static final String USERS_DIR = FILES_DIR + File.separator + "users";

	/** Archivo que almacena las credenciales de usuario */
	protected static final String USERS_FILE = USERS_DIR + File.separator + "users.txt";

	/** Puerto de control FTP estándar */
	protected static final int CONTROL_PORT = 21;

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
			return;
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
        		if (input.isBlank()) {
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
        Scanner sc = new Scanner(System.in);
        ServerConfig config = new ServerConfig();
        int serverPort = CONTROL_PORT;

        System.out.println(" _____                 _     _              _____ _____ _____");
        System.out.println("/  ___|               (_)   | |            |  ___|_   _| ___ \\");
        System.out.println("\\ `--.  ___ _ ____   ___  __| | ___  _ __  | |_    | | | |_/ /");
        System.out.println(" `--. \\/ _ \\ '__\\ \\ / / |/ _` |/ _ \\| '__| |  _|   | | |  __/ ");
        System.out.println("/\\__/ /  __/ |   \\ V /| | (_| | (_) | |    | |     | | | |    ");
        System.out.println("\\____/ \\___|_|    \\_/ |_|\\__,_|\\___/|_|    \\_|     \\_/ \\_|    ");
        System.out.println("\nBy Eduardo Díaz");

        // Inicializar sistema de logging
        FTP.Util.FileLogger.initialize();
        FTP.Util.FileLogger.info("========== SERVIDOR FTP INICIADO ==========");

        // Intentar cargar configuración desde archivo
        try {
            config.loadFromFile("server.properties");
            Util.printGreenColor("\n✓ Configuración cargada desde server.properties");
            serverPort = config.getControlPort();

            // Si hay directorio raíz en config, usarlo
            if (!config.getRootDirectory().isEmpty()) {
                dirRoot = config.getRootDirectory();
                Util.printGreenColor("✓ Directorio raíz: " + dirRoot);
            } else {
                checkDirs();
                setRoot(sc);
            }
        } catch (IOException e) {
            Util.printRedColor("\n⚠ No se pudo cargar server.properties, usando configuración por defecto");
            checkDirs();
            setRoot(sc);
        }

        execute = Executors.newCachedThreadPool();

        try (ServerSocket server = new ServerSocket(serverPort)) {
            Util.printGreenColor("\nServidor FTP iniciado en el puerto de control " + serverPort);
            FTP.Util.FileLogger.info("Servidor escuchando en puerto " + serverPort);

            while (true) {
                Socket client = server.accept();
                FTP.Util.FileLogger.logConnection(client.getInetAddress().toString());
                execute.execute(new Thread(new FtpClientHandler(client)));
            }

        } catch (IOException e) {
        	Util.printRedColor("\nError en el servidor: " + e.getMessage());
        	FTP.Util.FileLogger.error("Error en servidor: " + e.getMessage());
        } catch (Exception e) {
			Util.printRedColor("\nError: " + e.getMessage());
			FTP.Util.FileLogger.error("Error inesperado: " + e.getMessage());
			e.printStackTrace();
		}
    }
}