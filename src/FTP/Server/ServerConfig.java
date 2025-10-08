package FTP.Server;

import java.io.FileInputStream;
import java.io.IOException;
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

    /** Ruta al archivo de usuarios */
    private String usersFile;

    /** Máximo de conexiones concurrentes */
    private int maxConnections;

    /** Timeout de sesión en milisegundos */
    private long sessionTimeout;

    /** Habilitar logs verbosos */
    private boolean verboseLogging;

    /**
     * Constructor por defecto con valores predeterminados.
     */
    public ServerConfig() {
        // Valores por defecto
        this.controlPort = 21;
        this.rootDirectory = "";
        this.usersFile = "files/users/users.txt";
        this.maxConnections = 50;
        this.sessionTimeout = 300000; // 5 minutos
        this.verboseLogging = true;
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
        maxConnections = Integer.parseInt(properties.getProperty("ftp.max.connections", "50"));
        sessionTimeout = Long.parseLong(properties.getProperty("ftp.session.timeout", "300000"));
        verboseLogging = Boolean.parseBoolean(properties.getProperty("ftp.verbose.logging", "true"));
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

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }
}
