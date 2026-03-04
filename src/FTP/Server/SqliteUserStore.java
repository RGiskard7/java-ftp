package FTP.Server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Almacén de usuarios en SQLite. Crea el esquema si la tabla no existe.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class SqliteUserStore implements UserStore {

    private static final String TABLE = "ftp_users";
    private static final String CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "username TEXT UNIQUE NOT NULL, "
            + "password_hash TEXT NOT NULL, "
            + "profile TEXT NOT NULL CHECK(profile IN ('BASICO','INTERMEDIO','ADMINISTRADOR')), "
            + "enabled INTEGER NOT NULL DEFAULT 1, "
            + "created_at TEXT)";

    private final String dbPath;

    public SqliteUserStore(String dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * Asegura que la base existe y la tabla está creada. Llamar al arrancar el servidor.
     *
     * @throws SQLException Si no se puede crear/abrir la base o la tabla
     */
    public void initSchema() throws SQLException {
        try (Connection conn = getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute(CREATE_TABLE);
            }
        }
    }

    @Override
    public UserInfo findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT password_hash, profile, enabled FROM " + TABLE + " WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String hash = rs.getString(1);
                String profileStr = rs.getString(2);
                int enabled = rs.getInt(3);
                UserProfile profile;
                try {
                    profile = UserProfile.valueOf(profileStr);
                } catch (IllegalArgumentException e) {
                    return null;
                }
                return new UserInfo(username.trim(), hash, profile, enabled != 0);
            }
        } catch (SQLException e) {
            FTP.Util.FileLogger.error("SqliteUserStore findByUsername: " + e.getMessage());
            return null;
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + new File(dbPath).getAbsolutePath());
    }
}
