package FTP.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Sistema de logging a archivo para el servidor FTP.
 * <p>
 * Registra eventos importantes en un archivo de log para auditoría y debugging.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class FileLogger {
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = LOG_DIR + File.separator + "ftp-server.log";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static boolean enabled = true;

    /**
     * Inicializa el sistema de logging creando el directorio si no existe.
     */
    public static void initialize() {
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }

    /**
     * Registra un mensaje en el archivo de log.
     *
     * @param message Mensaje a registrar
     */
    public static void log(String message) {
        if (!enabled) return;

        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            String timestamp = dateFormat.format(new Date());
            pw.println("[" + timestamp + "] " + message);
        } catch (IOException e) {
            // Silenciosamente fallar si no se puede escribir el log
            System.err.println("⚠ Error escribiendo log: " + e.getMessage());
        }
    }

    /**
     * Registra un evento de información.
     *
     * @param message Mensaje de información
     */
    public static void info(String message) {
        log("[INFO] " + message);
    }

    /**
     * Registra un evento de error.
     *
     * @param message Mensaje de error
     */
    public static void error(String message) {
        log("[ERROR] " + message);
    }

    /**
     * Registra un evento de advertencia.
     *
     * @param message Mensaje de advertencia
     */
    public static void warning(String message) {
        log("[WARNING] " + message);
    }

    /**
     * Registra un evento de comando FTP.
     *
     * @param username Nombre del usuario
     * @param command Comando ejecutado
     */
    public static void logCommand(String username, String command) {
        log("[COMMAND] Usuario: " + username + " | Comando: " + command);
    }

    /**
     * Registra una conexión de cliente.
     *
     * @param clientAddress Dirección del cliente
     */
    public static void logConnection(String clientAddress) {
        log("[CONNECTION] Cliente conectado: " + clientAddress);
    }

    /**
     * Registra una desconexión de cliente.
     *
     * @param username Nombre del usuario
     * @param clientAddress Dirección del cliente
     */
    public static void logDisconnection(String username, String clientAddress) {
        log("[DISCONNECT] Usuario: " + username + " | Cliente: " + clientAddress);
    }

    /**
     * Registra un intento de autenticación.
     *
     * @param username Nombre del usuario
     * @param success Si fue exitoso
     */
    public static void logAuth(String username, boolean success) {
        String status = success ? "EXITOSO" : "FALLIDO";
        log("[AUTH] Usuario: " + username + " | " + status);
    }

    /**
     * Habilita o deshabilita el logging.
     *
     * @param enable true para habilitar, false para deshabilitar
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }
}
