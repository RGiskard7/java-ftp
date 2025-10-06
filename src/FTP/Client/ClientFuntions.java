package FTP.Client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import FTP.Util.Util;

/**
 * Implementación de las funciones del cliente FTP.
 * <p>
 * Esta clase encapsula todas las operaciones que el cliente puede realizar,
 * como listar archivos, subir/descargar, crear/eliminar directorios, etc.
 * Utiliza Apache Commons Net {@link FTPClient} para las comunicaciones FTP.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class ClientFuntions {
	/** Cliente FTP de Apache Commons Net */
	private FTPClient ftpClient;

	/**
	 * Constructor que inicializa las funciones del cliente.
	 *
	 * @param ftpClient Instancia del cliente FTP configurado
	 */
	public ClientFuntions(FTPClient ftpClient) {
		this.ftpClient = ftpClient;
	}

	/**
	 * Lista los archivos y directorios del directorio actual en el servidor.
	 * Muestra información formateada incluyendo tipo, tamaño, fecha y nombre.
	 */
	public void listFiles() {   
		FTPFile[] files;
		String type, size, date, name;
		
		try {
			System.out.println("\nSolicitando listado de archivos...");

			files = ftpClient.listFiles();
			 
			if (files.length == 0) {
			    System.out.println("\nEl directorio está vacío");
			}

	        // Imprimir cabecera con formato alineado
	        System.out.printf("\n%-12s %-10s %-15s %-20s\n", "TIPO", "TAMAÑO", "FECHA MOD.", "NOMBRE");
	        System.out.println("------------------------------------------------------------");

	        // Recorrer archivos y formatear la salida
	        for (FTPFile f : files) {
	            type = f.isDirectory() ? "<DIR>" : "FILE";
	            size = f.isDirectory() ? "-" : String.format("%,d", f.getSize());
	            date = f.getTimestamp().getTime().toString(); // Convertir fecha a String
	            name = f.getName();

	            // Imprimir fila formateada
	            System.out.printf("%-12s %-10s %-15s %-20s\n", type, size, date, name);
	        }
	        
		} catch (IOException e) {
			Util.printRedColor("\nError al listar los archivos del servidor");
		}
	}
	
	public void uploadFile(Scanner sc) {
		File localFile;
		String localFilePath, remoteFileName;
		
	    try {
	        System.out.println("\nIngrese la ruta completa del archivo a subir");
			System.out.print(":> ");
	        localFilePath = sc.nextLine().trim();
	        localFile = new File(localFilePath);
	        
	        if (!localFile.exists() || !localFile.isFile()) {
	        	Util.printRedColor("\nError: Archivo no encontrado o ruta incorrecta");
	            return;
	        }
	        
	        System.out.println("\nIngrese el nombre con el que se guardará en el servidor");
			System.out.print(":> ");
	        remoteFileName = sc.nextLine().trim();
	        
	        // Abrir el stream local y usar storeFile para enviar el contenido.
	        try (FileInputStream input = new FileInputStream(localFile)) {
	            boolean result = ftpClient.storeFile(remoteFileName, input);
	            if (result) {
	            	Util.printGreenColor("\nArchivo subido correctamente");
	            } else {
	            	Util.printRedColor("\nError en la subida del archivo: " + ftpClient.getReplyString());
	            }
	        }

	    } catch (IOException e) {
	    	Util.printRedColor("\nError: " + e.getMessage());
	    }
	}
	
	public void downloadFile(Scanner sc) {
	    System.out.println("\nIngrese el nombre del archivo a descargar");
	    System.out.print(":> ");
	    String remoteFileName = sc.nextLine().trim();
	    
	    System.out.println("\nIngrese la ruta local (incluyendo nombre de archivo) donde se guardará el archivo");
	    System.out.print(":> ");
	    String localFilePath = sc.nextLine().trim();
	    
	    File localFile = new File(localFilePath);
	    
	    try (FileOutputStream fos = new FileOutputStream(localFile)) {
	        // ftpClient.retrieveFile envía internamente el comando RETR y gestiona la conexión de datos
	        boolean result = ftpClient.retrieveFile(remoteFileName, fos);
	        if (result) {
	        	Util.printGreenColor("\nArchivo descargado correctamente");
	        } else {
	        	Util.printRedColor("\nError en la descarga del archivo: " + ftpClient.getReplyString());
	        }
	        
	    } catch (IOException e) {
	        System.out.println("\nError: " + e.getMessage());
	    }
	}
	
	public void deleteFile(Scanner sc) {
		String remoteFileName;
		
	    System.out.println("\nIngrese el nombre del archivo a eliminar");
	    System.out.print(":> ");
	    remoteFileName = sc.nextLine().trim();

	    if (remoteFileName.isEmpty()) {
	    	Util.printRedColor("\nError: El nombre del archivo no puede estar vacío.");
	        return;
	    }

	    try {
	        boolean success = ftpClient.deleteFile(remoteFileName);
	        if (success) {
	        	Util.printGreenColor("\nArchivo eliminado correctamente.");
	        } else {
	        	Util.printRedColor("\nError al eliminar el archivo: " + ftpClient.getReplyString());
	        }
	    } catch (IOException e) {
	    	Util.printRedColor("\nError: " + e.getMessage());
	    }
	}
	
	public void createDirectory(Scanner sc) {
	    System.out.println("\nIngrese el nombre del directorio a crear");
	    System.out.print(":> ");
	    String dirName = sc.nextLine().trim();

	    if (dirName.isEmpty()) {
	    	Util.printRedColor("\nError: El nombre del directorio no puede estar vacío");
	        return;
	    }

	    try {
	        boolean success = ftpClient.makeDirectory(dirName);
	        if (success) {
	        	Util.printGreenColor("\nDirectorio creado correctamente");
	        } else {
	        	Util.printRedColor("\nError al crear el directorio: " + ftpClient.getReplyString());
	        }

	    } catch (IOException e) {
	    	Util.printRedColor("\nError: " + e.getMessage());
	    }
	}
	
	public void deleteDirectory(Scanner sc) {
		String remoteDirName;
		
	    System.out.println("\nIngrese el nombre del directorio a eliminar");
	    System.out.print(":> ");
	    remoteDirName = sc.nextLine().trim();

	    if (remoteDirName.isEmpty()) {
	    	Util.printRedColor("\nError: El nombre del directorio no puede estar vacío.");
	        return;
	    }

	    try {
	        boolean success = ftpClient.removeDirectory(remoteDirName);
	        if (success) {
	        	Util.printGreenColor("\nDirectorio eliminado correctamente.");
	        } else {
	        	Util.printRedColor("\nError al eliminar el directorio: " + ftpClient.getReplyString());
	        }
	    } catch (IOException e) {
	    	Util.printRedColor("\nError: " + e.getMessage());
	    }
	}


	public void renameFile(Scanner sc) {
		String oldName, newName;
		
	    System.out.println("\nIngrese el nombre del archivo o directorio a renombrar");
	    System.out.print(":> ");
	    oldName = sc.nextLine().trim();

	    System.out.println("\nIngrese el nuevo nombre");
	    System.out.print(":> ");
	    newName = sc.nextLine().trim();

	    if (oldName.isEmpty() || newName.isEmpty()) {
	    	Util.printRedColor("\nError: Los nombres no pueden estar vacíos.");
	        return;
	    }

	    try {
	        // El método rename de Apache Commons Net envía internamente RNFR y RNTO.
	        boolean result = ftpClient.rename(oldName, newName);
	        if (result) {
	        	Util.printGreenColor("\nRenombrado correctamente.");
	        } else {
	        	Util.printRedColor("\nError al renombrar: " + ftpClient.getReplyString());
	        }
	    } catch (IOException e) {
	    	Util.printRedColor("\nError: " + e.getMessage());
	    }
	}
	
	public void changeWorkingDirectory(Scanner sc) {
		String dir;
		
	    System.out.println("\nIngrese el directorio al que desea cambiar:");
	    System.out.print(":> ");
	    dir = sc.nextLine().trim();
	    
	    if (dir.isEmpty()) {
	    	Util.printRedColor("\nError: El nombre del directorio no puede estar vacío.");
	        return;
	    }
	    
	    try {
	    	
	        // ftpClient.changeWorkingDirectory envía el comando CWD al servidor
	        boolean success = ftpClient.changeWorkingDirectory(dir);
	        showServerReply(ftpClient);
	        
	        if (success) {
	        	Util.printGreenColor("\nDirectorio cambiado a: " + ftpClient.printWorkingDirectory());
	        } else {
	        	Util.printRedColor("\nError al cambiar de directorio: " + ftpClient.getReplyString());
	        }
	    } catch (IOException e) {
	    	Util.printRedColor("\nError: " + e.getMessage());
	    }
	}
	
	public void changeToParentDirectory() {
	    try {
	        boolean success = ftpClient.changeToParentDirectory();
	        if (success) {
	        	Util.printGreenColor("\nDirectorio cambiado al directorio superior: " + ftpClient.printWorkingDirectory());
	        } else {
	        	Util.printRedColor("\nError al cambiar al directorio superior: " + ftpClient.getReplyString());
	        }
	    } catch (IOException e) {
	    	Util.printRedColor("\nError: " + e.getMessage());
	    }
	}
	
    private static void showServerReply(FTPClient ftpClient) {
        String[] replies = ftpClient.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                System.out.println("SERVER: " + aReply);
            }
        }
    }
}
