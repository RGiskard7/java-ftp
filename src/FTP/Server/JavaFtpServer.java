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
 * @author Eduardo Díaz Sánchez
 */
public class JavaFtpServer {
	protected static String dirRoot;
	protected static final String FILES_DIR = "files";
	protected static final String USERS_DIR = FILES_DIR + File.separator + "users";
	protected static final String USERS_FILE = USERS_DIR + File.separator + "users.txt";
	protected static final int CONTROL_PORT = 21;
	
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
    
    public static void main(String[] args) throws InterruptedException {
        ExecutorService execute = null;
        Scanner sc = new Scanner(System.in);

        
        System.out.println(" _____                 _     _              _____ _____ _____");
		System.out.println("/  ___|               (_)   | |            |  ___|_   _| ___ \\");
		System.out.println("\\ `--.  ___ _ ____   ___  __| | ___  _ __  | |_    | | | |_/ /");
		System.out.println(" `--. \\/ _ \\ '__\\ \\ / / |/ _` |/ _ \\| '__| |  _|   | | |  __/ ");
		System.out.println("/\\__/ /  __/ |   \\ V /| | (_| | (_) | |    | |     | | | |    ");
		System.out.println("\\____/ \\___|_|    \\_/ |_|\\__,_|\\___/|_|    \\_|     \\_/ \\_|    ");
		System.out.println("\nBy Eduardo Díaz");
		
		checkDirs();
		setRoot(sc);
        
        execute = Executors.newCachedThreadPool();
       
        try (ServerSocket server = new ServerSocket(CONTROL_PORT)) {
            Util.printGreenColor("\nServidor FTP iniciado en el puerto de control " + CONTROL_PORT);
            
            while (true) {
                Socket client = server.accept();
                execute.execute(new Thread(new FtpClientHandler(client)));
            }

        } catch (IOException e) {
        	Util.printRedColor("\nError en el servidor: " + e.getMessage());
        } catch (Exception e) {
			Util.printRedColor("\nError: " + e.getMessage());
			e.printStackTrace();
		} 
    }
}