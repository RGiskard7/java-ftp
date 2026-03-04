package FTP.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Herramienta de migración: lee users.txt (formato username:bcryptHash:profile)
 * e inserta los usuarios en la base SQLite. Crea la base y la tabla si no existen.
 * <p>
 * Uso:
 * <pre>
 *   java ... FTP.Server.MigrateUsersToDb [ruta-users.txt] [ruta-salida.db]
 * </pre>
 * Si se omite la ruta de salida, se usa ftp.users.database de server.properties.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class MigrateUsersToDb {

    private static final String TABLE = "ftp_users";
    private static final String CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "username TEXT UNIQUE NOT NULL, "
            + "password_hash TEXT NOT NULL, "
            + "profile TEXT NOT NULL CHECK(profile IN ('BASICO','INTERMEDIO','ADMINISTRADOR')), "
            + "enabled INTEGER NOT NULL DEFAULT 1, "
            + "created_at TEXT)";

    public static void main(String[] args) {
        String txtPath = args.length >= 1 ? args[0] : "files/users/users.txt";
        String dbPath;
        if (args.length >= 2) {
            dbPath = args[1];
        } else {
            try {
                Properties p = new Properties();
                try (FileInputStream fis = new FileInputStream("server.properties")) {
                    p.load(fis);
                }
                dbPath = p.getProperty("ftp.users.database", "").trim();
                if (dbPath.isEmpty()) {
                    dbPath = "files/ftp_users.db";
                }
            } catch (IOException e) {
                dbPath = "files/ftp_users.db";
            }
        }

        File txtFile = new File(txtPath);
        if (!txtFile.exists() || !txtFile.isFile()) {
            System.err.println("ERROR: No existe el fichero de usuarios: " + txtPath);
            return;
        }

        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            File parent = dbFile.getParentFile();
            if (parent != null) parent.mkdirs();
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
            try (Statement st = conn.createStatement()) {
                st.execute(CREATE_TABLE);
            }
            String sql = "INSERT OR REPLACE INTO " + TABLE + " (username, password_hash, profile, enabled, created_at) VALUES (?, ?, ?, 1, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int count = 0;
                try (FileInputStream fis = new FileInputStream(txtFile);
                     InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                     BufferedReader br = new BufferedReader(isr)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split(":", 3);
                        if (parts.length != 3) {
                            System.err.println("Línea ignorada (se esperan 3 campos): " + line);
                            continue;
                        }
                        String username = parts[0].trim();
                        String hash = parts[1].trim();
                        String profileStr = parts[2].trim();
                        try {
                            UserProfile.valueOf(profileStr);
                        } catch (IllegalArgumentException e) {
                            System.err.println("Perfil no válido '" + profileStr + "' en: " + line);
                            continue;
                        }
                        if (username.isEmpty()) {
                            System.err.println("Usuario vacío en: " + line);
                            continue;
                        }
                        ps.setString(1, username);
                        ps.setString(2, hash);
                        ps.setString(3, profileStr);
                        ps.setString(4, null);
                        ps.executeUpdate();
                        count++;
                    }
                }
                System.out.println("Migración completada: " + count + " usuarios en " + dbPath);
            }
        } catch (SQLException e) {
            System.err.println("ERROR SQLite: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("ERROR leyendo " + txtPath + ": " + e.getMessage());
        }
    }
}
