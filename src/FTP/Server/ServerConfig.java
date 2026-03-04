package FTP.Server;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Properties;

/**
 * Configuración del servidor FTP cargada desde archivo server.properties.
 * <p>
 * Permite configurar el servidor sin necesidad de recompilar el código.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class ServerConfig {
    private Properties properties;

    /** Puerto de control FTP */
    private int controlPort;

    /** Directorio raíz del servidor */
    private String rootDirectory;

    /** Ruta al archivo de usuarios (modo legacy TXT) */
    private String usersFile;

    /** Ruta a la base SQLite de usuarios (si no vacía, se usa en lugar de usersFile) */
    private String usersDatabase;

    /** Máximo de conexiones concurrentes */
    private int maxConnections;

    /** Timeout de sesión en milisegundos */
    private long sessionTimeout;

    /** Habilitar logs verbosos */
    private boolean verboseLogging;

    /** Puerto mínimo para modo pasivo (0 = usar puerto aleatorio) */
    private int passivePortMin;

    /** Puerto máximo para modo pasivo (0 = usar puerto aleatorio) */
    private int passivePortMax;

    /** Máximo de intentos de login fallidos por IP antes de bloqueo */
    private int authMaxAttempts;

    /** Minutos de bloqueo tras superar authMaxAttempts (0 = desactivado) */
    private int authLockoutMinutes;

    /** TLS habilitado */
    private boolean tlsEnabled;

    /** Ruta al keystore JKS/PKCS12 */
    private String tlsKeystorePath;

    /** Contraseña del keystore */
    private String tlsKeystorePassword;

    /** Protocolos TLS (ej. TLSv1.2,TLSv1.3) */
    private String tlsProtocols;

    /** Si true, rechazar conexiones que no hagan AUTH TLS */
    private boolean tlsRequired;

    /** SSLContext cargado (null si TLS desactivado) */
    private SSLContext sslContext;

    /** Tamaño máximo del log en bytes antes de rotar (0 = no rotar) */
    private long logMaxSizeBytes;

    /** Número de archivos de respaldo de log */
    private int logMaxBackupFiles;

    /**
     * Constructor por defecto con valores predeterminados.
     */
    public ServerConfig() {
        // Valores por defecto
        this.controlPort = 21;
        this.rootDirectory = "";
        this.usersFile = "files/users/users.txt";
        this.usersDatabase = "";
        this.maxConnections = 50;
        this.sessionTimeout = 300000; // 5 minutos
        this.verboseLogging = true;
        this.passivePortMin = 0;
        this.passivePortMax = 0;
        this.authMaxAttempts = 5;
        this.authLockoutMinutes = 5;
        this.tlsEnabled = false;
        this.tlsKeystorePath = "";
        this.tlsKeystorePassword = "";
        this.tlsProtocols = "TLSv1.2";
        this.tlsRequired = false;
        this.logMaxSizeBytes = 5 * 1024 * 1024; // 5 MB
        this.logMaxBackupFiles = 3;
    }

    /**
     * Carga la configuración desde el archivo server.properties.
     *
     * @param configFile Ruta al archivo de configuración
     * @throws IOException Si hay error al leer el archivo
     */
    public void loadFromFile(String configFile) throws IOException {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        }

        // Leer propiedades con valores por defecto
        controlPort = Integer.parseInt(properties.getProperty("ftp.control.port", "21"));
        rootDirectory = properties.getProperty("ftp.root.directory", "");
        usersFile = properties.getProperty("ftp.users.file", "files/users/users.txt");
        String db = properties.getProperty("ftp.users.database", "").trim();
        usersDatabase = db.isEmpty() ? "" : db;
        maxConnections = Integer.parseInt(properties.getProperty("ftp.max.connections", "50"));
        sessionTimeout = Long.parseLong(properties.getProperty("ftp.session.timeout", "300000"));
        verboseLogging = Boolean.parseBoolean(properties.getProperty("ftp.verbose.logging", "true"));
        String range = properties.getProperty("ftp.passive.port.range", "").trim();
        if (!range.isEmpty() && range.contains("-")) {
            String[] parts = range.split("-", 2);
            try {
                passivePortMin = Integer.parseInt(parts[0].trim());
                passivePortMax = Integer.parseInt(parts[1].trim());
                if (passivePortMin > passivePortMax) {
                    int t = passivePortMin;
                    passivePortMin = passivePortMax;
                    passivePortMax = t;
                }
            } catch (NumberFormatException e) {
                passivePortMin = 0;
                passivePortMax = 0;
            }
        }
        authMaxAttempts = Integer.parseInt(properties.getProperty("ftp.auth.max.attempts", "5"));
        authLockoutMinutes = Integer.parseInt(properties.getProperty("ftp.auth.lockout.minutes", "5"));
        tlsEnabled = Boolean.parseBoolean(properties.getProperty("ftp.tls.enabled", "false"));
        tlsKeystorePath = properties.getProperty("ftp.tls.keystore.path", "").trim();
        tlsKeystorePassword = properties.getProperty("ftp.tls.keystore.password", "");
        tlsProtocols = properties.getProperty("ftp.tls.protocols", "TLSv1.2").trim();
        tlsRequired = Boolean.parseBoolean(properties.getProperty("ftp.tls.required", "false"));
        if (tlsEnabled && !tlsKeystorePath.isEmpty()) {
            try {
                sslContext = loadSSLContext();
            } catch (Exception e) {
                throw new IOException("No se pudo cargar TLS: " + e.getMessage(), e);
            }
        }
        logMaxSizeBytes = Long.parseLong(properties.getProperty("ftp.log.max.size.bytes", "5242880"));
        logMaxBackupFiles = Integer.parseInt(properties.getProperty("ftp.log.max.backups", "3"));
    }

    private SSLContext loadSSLContext() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(tlsKeystorePath)) {
            ks.load(fis, tlsKeystorePassword.isEmpty() ? null : tlsKeystorePassword.toCharArray());
        }
        javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory.getInstance(
            javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, tlsKeystorePassword.isEmpty() ? new char[0] : tlsKeystorePassword.toCharArray());
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx;
    }

    // Getters
    public int getControlPort() {
        return controlPort;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public String getUsersFile() {
        return usersFile;
    }

    public String getUsersDatabase() {
        return usersDatabase;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public boolean isVerboseLogging() {
        return verboseLogging;
    }

    // Setters (para permitir configuración manual si no hay archivo)
    public void setControlPort(int controlPort) {
        this.controlPort = controlPort;
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public void setUsersFile(String usersFile) {
        this.usersFile = usersFile;
    }

    public void setUsersDatabase(String usersDatabase) {
        this.usersDatabase = usersDatabase == null ? "" : usersDatabase.trim();
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }

    public int getPassivePortMin() {
        return passivePortMin;
    }

    public int getPassivePortMax() {
        return passivePortMax;
    }

    public void setPassivePortRange(int min, int max) {
        this.passivePortMin = min;
        this.passivePortMax = max;
    }

    public int getAuthMaxAttempts() {
        return authMaxAttempts;
    }

    public int getAuthLockoutMinutes() {
        return authLockoutMinutes;
    }

    public boolean isTlsEnabled() { return tlsEnabled; }
    public String getTlsKeystorePath() { return tlsKeystorePath; }
    public String getTlsProtocols() { return tlsProtocols; }
    public boolean isTlsRequired() { return tlsRequired; }
    public SSLContext getSslContext() { return sslContext; }

    public long getLogMaxSizeBytes() { return logMaxSizeBytes; }
    public int getLogMaxBackupFiles() { return logMaxBackupFiles; }
}
