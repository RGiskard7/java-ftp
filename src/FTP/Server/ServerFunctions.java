package FTP.Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import FTP.Util.Util;

/**
 * @author Eduardo Díaz Sánchez
 */
public class ServerFunctions {
	private FtpClientHandler handler;    
    private File pendingRenameFile; // Variable para almacenar el archivo/directorio RNFR
	
    public ServerFunctions(FtpClientHandler handler) {
        this.handler = handler;
    }
	
    protected void handleListCommand() {
    	Socket dataSocket = null;
    	PrintWriter dataOut = null;
    	File dir;
    	File[] files;
    	
    	handler.sendReply(150, "Here comes the directory listing.");
    	
		try {
			dataSocket = handler.getDataSocket(); // Obtener la conexión de datos
			dataOut = new PrintWriter(dataSocket.getOutputStream(), true);
			
			dir = new File(handler.getCurrentDirectory());
			files = dir.listFiles();
			
	        if (files == null) {
	        	handler.sendReply(550, "Directory not found.");
	            return;
	        }
	        
	        for (File file : files) {
	            String permissions = file.isDirectory() ? "drwxr-xr-x" : "-rw-r--r--";
	            String owner = "ftp";
	            String group = "ftp";
	            long size = file.length();
	            String date = "Jan 1 00:00"; // Fecha simplificada
	            String name = file.getName();

	            String line = String.format("%s 1 %s %s %d %s %s",
	                    permissions, owner, group, size, date, name);
	            dataOut.println(line);
	        }

		} catch (IOException e) {
			handler.sendReply(426, "Connection closed; transfer aborted.");
			
		} finally {
			if (dataOut != null) {
				dataOut.flush();
				dataOut.close();
			}
			
			//closeDataSocket();
	        if (dataSocket != null) {
	            try {
	                dataSocket.close();
	            } catch (IOException e) {
	            	Util.printRedColor("Error al cerrar socket de datos: " + e.getMessage());
	            }
	        }
	        
	        handler.sendReply(226, "Directory send OK."); // Se envía después de cerrar sockets
		}
	}
    
    protected void handleUploadFileCommand(String filename) {
        Socket dataSocket = null;
        FileOutputStream fos = null;
        InputStream dataIn;
        File file;
    	
        if (filename == null || filename.isBlank()) {
        	handler.sendReply(501, "Syntax error in parameters or arguments.");
            return;
        }
        
        handler.sendReply(150, "Opening data connection.");

        try {
            dataSocket = handler.getDataSocket(); // Establece la conexión de datos (activo o pasivo)
            dataIn = dataSocket.getInputStream();
            
            // Construye la ruta de destino (puedes añadir lógica para evitar sobrescrituras, etc.)
            file = new File(JavaFtpServer.dirRoot, filename);
            fos = new FileOutputStream(file);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dataIn.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            
            fos.flush();
            handler.sendReply(226, "Transfer complete.");
            
        } catch (IOException e) {
        	handler.sendReply(426, "Connection closed; transfer aborted.");
        	Util.printRedColor("\nError en upload: " + e.getMessage());
            
        } finally {
            try {
                if (fos != null) fos.close();
                if (dataSocket != null) dataSocket.close();
            } catch (IOException e) {
            	Util.printRedColor("\nError al cerrar stream/socket: " + e.getMessage());
            }
        }
    }
    
    protected void handleDownloadFileCommand(String filename) {
        Socket dataSocket = null;
        FileInputStream fis = null;
        OutputStream dataOut;
        File file;
    	
        if (filename == null || filename.isBlank()) {
            handler.sendReply(501, "Syntax error in parameters or arguments.");
            return;
        }
        
        // Construir la ruta del archivo a descargar
        file = new File(JavaFtpServer.dirRoot, filename);
        if (!file.exists() || !file.isFile()) {
            handler.sendReply(550, "File not found.");
            return;
        }
        
        // Indicar que se va a abrir la conexión de datos para descargar el archivo
        handler.sendReply(150, "Opening data connection.");
        
        try {
            dataSocket = handler.getDataSocket();
            fis = new FileInputStream(file);
            // Obtener el stream de salida del socket de datos
            dataOut = dataSocket.getOutputStream();
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dataOut.write(buffer, 0, bytesRead);
            }
            
            dataOut.flush();
            
            handler.sendReply(226, "Transfer complete.");
            
        } catch (IOException e) {
            handler.sendReply(426, "Connection closed; transfer aborted.");
            Util.printRedColor("Error en download: " + e.getMessage());
            
        } finally {
            try {
                if (fis != null) fis.close();
                if (dataSocket != null) dataSocket.close();
            } catch (IOException e) {
            	Util.printRedColor("Error al cerrar stream/socket: " + e.getMessage());
            }
        }
    }
    
    protected void handleDeleteFileCommand(String filename) {
    	File file;
    	
        if (filename == null || filename.isBlank()) {
            handler.sendReply(501, "Syntax error in parameters or arguments.");
            return;
        }

        file = new File(JavaFtpServer.dirRoot, filename);

        // Verificar si el archivo existe y no es un directorio
        if (!file.exists() || !file.isFile()) {
            handler.sendReply(550, "File not found.");
            return;
        }

        if (file.delete()) {
            handler.sendReply(250, "File deleted successfully.");
        } else {
            handler.sendReply(450, "File deletion failed.");
        }
    }
    
    protected void handleCreateDirectory(String dirName) {
    	File newDir;
    	
        if (dirName == null || dirName.isBlank()) {
            handler.sendReply(501, "Syntax error in parameters or arguments.");
            return;
        }

        newDir = new File(JavaFtpServer.dirRoot, dirName);

        if (newDir.exists()) {
            handler.sendReply(550, "Directory already exists.");
            return;
        }

        if (newDir.mkdirs()) {
            handler.sendReply(257, "\"" + dirName + "\" directory created successfully.");
        } else {
            handler.sendReply(550, "Failed to create directory.");
        }
    }
    
    protected void handleDeleteDirectoryCommand(String dirName) {
    	File dir;
    	
        if (dirName == null || dirName.isBlank()) {
            handler.sendReply(501, "Syntax error in parameters or arguments.");
            return;
        }
        
        dir = new File(JavaFtpServer.dirRoot, dirName);
        
        // Verificar que exista y que sea un directorio
        if (!dir.exists() || !dir.isDirectory()) {
            handler.sendReply(550, "Directory not found.");
            return;
        }
        
        // Intentar eliminar el directorio
        // File.delete() sólo elimina directorios vacIos
        if (dir.delete()) {
            handler.sendReply(250, "Directory deleted successfully.");
        } else {
            handler.sendReply(450, "Directory deletion failed.");
        }
    }
    
    protected void handleRenameFromCommand(String oldName) {
    	File file;
    	
        if (oldName == null || oldName.isBlank()) {
            handler.sendReply(501, "Syntax error in parameters or arguments.");
            return;
        }

        file = new File(JavaFtpServer.dirRoot, oldName);
        if (!file.exists()) {
            handler.sendReply(550, "File or directory not found.");
            return;
        }

        // Guardar el archivo pendiente para renombrar y enviar respuesta 350
        pendingRenameFile = file;
        handler.sendReply(350, "File exists, ready for destination name.");
    }
    
    protected void handleRenameToCommand(String newName) {
    	File destFile;
    	
        if (newName == null || newName.isBlank()) {
            handler.sendReply(501, "Syntax error in parameters or arguments.");
            return;
        }

        // Verificar que se haya recibido previamente un RNFR
        if (pendingRenameFile == null) {
            handler.sendReply(503, "Bad sequence of commands.");
            return;
        }

        destFile = new File(JavaFtpServer.dirRoot, newName);

        // Intentar renombrar
        if (pendingRenameFile.renameTo(destFile)) {
            handler.sendReply(250, "File or directory renamed successfully.");
        } else {
            handler.sendReply(550, "File or directory rename failed.");
        }
        
        // Limpiar el nombre del fichero pendiente
        pendingRenameFile = null;
    }
    
    protected void handleChangeWorkingDirectory(String dir) {
    	File newDir;
    	
        if (dir == null || dir.isBlank()) {
            handler.sendReply(501, "Syntax error in parameters or arguments.");
            return;
        }
        
        // Construir la ruta del nuevo directorio 
        newDir = new File(handler.getCurrentDirectory(), dir);
        
        if (!newDir.exists() || !newDir.isDirectory()) {
            handler.sendReply(550, "Directory not found or not a directory.");
            return;
        }
        
        // Actualizar el directorio de trabajo del cliente
        handler.setCurrentDirectory(newDir.getAbsolutePath());
        handler.sendReply(250, "Directory successfully changed to " + newDir.getName());
    }
    
    protected void handleChangeToParentDirectory() {
    	// Directorio actual del cliente
        String currentDir = handler.getCurrentDirectory();
        File current = new File(currentDir);
        File parent = current.getParentFile();
        File root = new File(JavaFtpServer.dirRoot); // Definimos el directorio raíz del servidor
        
        // Si no hay directorio padre o el padre está fuera del directorio raíz, no se permite el cambio
        if (parent == null || !parent.getAbsolutePath().startsWith(root.getAbsolutePath())) {
            handler.sendReply(550, "Failed to change to parent directory.");
            return;
        }
        
        // Actualizamos el directorio de trabajo
        handler.setCurrentDirectory(parent.getAbsolutePath());
        handler.sendReply(200, "Directory successfully changed to parent directory.");
    }
}
