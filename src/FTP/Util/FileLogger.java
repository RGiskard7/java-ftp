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
    /** Si false, no se registran eventos detallados (p. ej. por comando) */
    private static boolean verbose = true;
    /** Tamaño máximo del archivo de log en bytes antes de rotar (0 = no rotar) */
    private static long maxFileSizeBytes = 0;
    /** Número de archivos de respaldo a conservar (0 = solo el actual) */
    private static int maxBackupFiles = 3;

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
     * Configura la rotación de logs por tamaño.
     *
     * @param maxBytes Tamaño máximo en bytes (0 = desactivado)
     * @param backupCount Número de archivos de respaldo a conservar
     */
    public static void setRotation(long maxBytes, int backupCount) {
        maxFileSizeBytes = maxBytes;
        maxBackupFiles = Math.max(0, backupCount);
    }

    private static void rotateIfNeeded() {
        if (maxFileSizeBytes <= 0) return;
        File f = new File(LOG_FILE);
        if (!f.exists() || f.length() < maxFileSizeBytes) return;
        try {
            for (int i = maxBackupFiles; i >= 1; i--) {
                File old = new File(LOG_FILE + "." + i);
                if (i == 1) {
                    File current = new File(LOG_FILE);
                    if (current.exists())
                        current.renameTo(old);
                } else {
                    File prev = new File(LOG_FILE + "." + (i - 1));
                    if (prev.exists())
                        prev.renameTo(old);
                }
            }
        } catch (Exception ignored) { }
    }

    /**
     * Registra un mensaje en el archivo de log.
     *
     * @param message Mensaje a registrar
     */
    public static void log(String message) {
        if (!enabled) return;

        rotateIfNeeded();
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            String timestamp = dateFormat.format(new Date());
            pw.println("[" + timestamp + "] " + message);
        } catch (IOException e) {
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
     * Registra un evento de comando FTP (solo si verbose está activo).
     *
     * @param username Nombre del usuario
     * @param command Comando ejecutado
     */
    public static void logCommand(String username, String command) {
        if (verbose) log("[COMMAND] Usuario: " + username + " | Comando: " + command);
    }

    /**
     * Activa o desactiva el logging detallado (comandos, etc.).
     *
     * @param v true para logging detallado
     */
    public static void setVerbose(boolean v) {
        verbose = v;
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
     * Registra un intento de login bloqueado por rate limiting.
     *
     * @param clientAddress IP del cliente
     */
    public static void logAuthBlocked(String clientAddress) {
        log("[AUTH] BLOQUEADO por límite de intentos: " + clientAddress);
    }

    /**
     * Registra un evento de auditoría (comando sensible o rechazo).
     *
     * @param username Usuario (o "-" si no autenticado)
     * @param clientAddress IP del cliente
     * @param action Acción (STOR, DELE, RMD, MKD, RNFR, RNTO, PERM_DENIED, PATH_DENIED)
     * @param detail Detalle (nombre de archivo, etc.; sin contraseñas)
     */
    public static void logAudit(String username, String clientAddress, String action, String detail) {
        log("[AUDIT] " + username + " | " + clientAddress + " | " + action + " | " + detail);
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
