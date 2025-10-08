package FTP.Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import FTP.Util.Util;

/**
 * Implementación de las funciones del servidor FTP.
 * <p>
 * Esta clase encapsula la lógica de todos los comandos FTP del servidor,
 * incluyendo operaciones de archivos, directorios y transferencias de datos.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class ServerFunctions {
	/** Referencia al manejador del cliente FTP */
	private FtpClientHandler handler;

	/** Archivo/directorio pendiente para operación de renombrado (comando RNFR) */
    private File pendingRenameFile;

	/**
	 * Constructor que inicializa las funciones del servidor.
	 *
	 * @param handler Manejador del cliente FTP asociado
	 */
    public ServerFunctions(FtpClientHandler handler) {
        this.handler = handler;
    }

	/**
	 * Valida que una ruta de archivo esté dentro del directorio raíz del servidor.
	 * Previene ataques de path traversal (../).
	 *
	 * @param file Archivo a validar
	 * @return true si el archivo está dentro del directorio raíz, false en caso contrario
	 */
	private boolean isPathSafe(File file) {
		try {
			File rootDir = new File(JavaFtpServer.dirRoot).getCanonicalFile();
			File targetFile = file.getCanonicalFile();

			// Verificar que el path del archivo comience con el path del directorio raíz
			return targetFile.getAbsolutePath().startsWith(rootDir.getAbsolutePath());
		} catch (IOException e) {
			Util.printRedColor("Error al validar path: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Valida que un nombre de archivo no contenga caracteres peligrosos.
	 * Previene inyección de comandos y path traversal.
	 *
	 * @param filename Nombre de archivo a validar
	 * @return true si el nombre es seguro, false en caso contrario
	 */
	private boolean isFilenameSafe(String filename) {
		if (filename == null || filename.isEmpty()) {
			return false;
		}

		// Rechazar nombres que contengan path separators o caracteres peligrosos
		// Nota: & se permite ya que solo es peligroso en shells, no en filesystems
		if (filename.contains("..") || filename.contains("/") || filename.contains("\\") ||
			filename.contains("\0") || filename.contains("|") || filename.contains(">") ||
			filename.contains("<")) {
			return false;
		}

		return true;
	}

	/**
	 * Maneja el comando LIST para listar archivos y directorios.
	 * Envía la lista de archivos del directorio actual al cliente a través de la conexión de datos.
	 */
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

	        SimpleDateFormat recentFormat = new SimpleDateFormat("MMM dd HH:mm");
	        SimpleDateFormat oldFormat = new SimpleDateFormat("MMM dd  yyyy");
	        long currentTime = System.currentTimeMillis();
	        long sixMonthsInMillis = 6L * 30L * 24L * 60L * 60L * 1000L; // Aproximadamente 6 meses

	        for (File file : files) {
	            String permissions = file.isDirectory() ? "drwxr-xr-x" : "-rw-r--r--";
	            String owner = "ftp";
	            String group = "ftp";
	            long size = file.length();
	            long lastModified = file.lastModified();

	            // Usar formato con hora si el archivo fue modificado en los últimos 6 meses
	            // Usar formato con año si el archivo es más antiguo de 6 meses
	            String date;
	            if (currentTime - lastModified < sixMonthsInMillis && lastModified <= currentTime) {
	                date = recentFormat.format(new Date(lastModified));
	            } else {
	                date = oldFormat.format(new Date(lastModified));
	            }

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

        // Validar que el nombre de archivo sea seguro
        if (!isFilenameSafe(filename)) {
            handler.sendReply(553, "File name not allowed.");
            Util.printRedColor("Nombre de archivo peligroso rechazado: " + filename);
            return;
        }

        handler.sendReply(150, "Opening data connection.");

        try {
            dataSocket = handler.getDataSocket(); // Establece la conexión de datos (activo o pasivo)
            dataIn = dataSocket.getInputStream();

            // Construye la ruta de destino
            file = new File(handler.getCurrentDirectory(), filename);

            // Validar que el archivo esté dentro del directorio raíz
            if (!isPathSafe(file)) {
                handler.sendReply(550, "Access denied. Path outside root directory.");
                Util.printRedColor("Intento de path traversal en upload: " + file.getAbsolutePath());
                return;
            }

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

        // Validar que el nombre de archivo sea seguro
        if (!isFilenameSafe(filename)) {
            handler.sendReply(553, "File name not allowed.");
            Util.printRedColor("Nombre de archivo peligroso rechazado: " + filename);
            return;
        }

        // Construir la ruta del archivo a descargar
        file = new File(handler.getCurrentDirectory(), filename);

        // Validar que el archivo esté dentro del directorio raíz
        if (!isPathSafe(file)) {
            handler.sendReply(550, "Access denied. Path outside root directory.");
            Util.printRedColor("Intento de path traversal en download: " + file.getAbsolutePath());
            return;
        }

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

        // Validar que el nombre de archivo sea seguro
        if (!isFilenameSafe(filename)) {
            handler.sendReply(553, "File name not allowed.");
            Util.printRedColor("Nombre de archivo peligroso rechazado: " + filename);
            return;
        }

        file = new File(handler.getCurrentDirectory(), filename);

        // Validar que el archivo esté dentro del directorio raíz
        if (!isPathSafe(file)) {
            handler.sendReply(550, "Access denied. Path outside root directory.");
            Util.printRedColor("Intento de path traversal en delete: " + file.getAbsolutePath());
            return;
        }

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

        // Validar que el nombre de directorio sea seguro
        if (!isFilenameSafe(dirName)) {
            handler.sendReply(553, "Directory name not allowed.");
            Util.printRedColor("Nombre de directorio peligroso rechazado: " + dirName);
            return;
        }

        newDir = new File(handler.getCurrentDirectory(), dirName);

        // Validar que el directorio esté dentro del directorio raíz
        if (!isPathSafe(newDir)) {
            handler.sendReply(550, "Access denied. Path outside root directory.");
            Util.printRedColor("Intento de path traversal en MKD: " + newDir.getAbsolutePath());
            return;
        }

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

        // Validar que el nombre de directorio sea seguro
        if (!isFilenameSafe(dirName)) {
            handler.sendReply(553, "Directory name not allowed.");
            Util.printRedColor("Nombre de directorio peligroso rechazado: " + dirName);
            return;
        }

        dir = new File(handler.getCurrentDirectory(), dirName);

        // Validar que el directorio esté dentro del directorio raíz
        if (!isPathSafe(dir)) {
            handler.sendReply(550, "Access denied. Path outside root directory.");
            Util.printRedColor("Intento de path traversal en RMD: " + dir.getAbsolutePath());
            return;
        }

        // Verificar que exista y que sea un directorio
        if (!dir.exists() || !dir.isDirectory()) {
            handler.sendReply(550, "Directory not found.");
            return;
        }

        // Intentar eliminar el directorio
        // File.delete() sólo elimina directorios vacíos
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

        // Validar que el nombre sea seguro
        if (!isFilenameSafe(oldName)) {
            handler.sendReply(553, "File name not allowed.");
            Util.printRedColor("Nombre de archivo peligroso rechazado: " + oldName);
            return;
        }

        file = new File(handler.getCurrentDirectory(), oldName);

        // Validar que el archivo esté dentro del directorio raíz
        if (!isPathSafe(file)) {
            handler.sendReply(550, "Access denied. Path outside root directory.");
            Util.printRedColor("Intento de path traversal en RNFR: " + file.getAbsolutePath());
            return;
        }

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

        // Validar que el nombre sea seguro
        if (!isFilenameSafe(newName)) {
            handler.sendReply(553, "File name not allowed.");
            Util.printRedColor("Nombre de archivo peligroso rechazado: " + newName);
            pendingRenameFile = null; // Limpiar estado
            return;
        }

        // Verificar que se haya recibido previamente un RNFR
        if (pendingRenameFile == null) {
            handler.sendReply(503, "Bad sequence of commands.");
            return;
        }

        destFile = new File(handler.getCurrentDirectory(), newName);

        // Validar que el archivo esté dentro del directorio raíz
        if (!isPathSafe(destFile)) {
            handler.sendReply(550, "Access denied. Path outside root directory.");
            Util.printRedColor("Intento de path traversal en RNTO: " + destFile.getAbsolutePath());
            pendingRenameFile = null; // Limpiar estado
            return;
        }

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

        // Validar que el nuevo directorio esté dentro del directorio raíz
        if (!isPathSafe(newDir)) {
            handler.sendReply(550, "Access denied. Path outside root directory.");
            Util.printRedColor("Intento de path traversal detectado: " + newDir.getAbsolutePath());
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

	/**
	 * Maneja el comando PWD para mostrar el directorio de trabajo actual.
	 * Envía el directorio actual al cliente en formato estándar FTP.
	 */
    protected void handlePrintWorkingDirectory() {
        try {
            String currentDir = handler.getCurrentDirectory();
            File current = new File(currentDir);
            File root = new File(JavaFtpServer.dirRoot);

            // Calcular la ruta relativa desde el directorio raíz
            String relativePath = current.getAbsolutePath()
                .substring(root.getAbsolutePath().length())
                .replace("\\", "/");

            // Si está en el directorio raíz, mostrar "/"
            if (relativePath.isEmpty()) {
                relativePath = "/";
            }

            handler.sendReply(257, "\"" + relativePath + "\" is the current directory.");
        } catch (Exception e) {
            handler.sendReply(550, "Failed to get current directory.");
            Util.printRedColor("Error en PWD: " + e.getMessage());
        }
    }
}
