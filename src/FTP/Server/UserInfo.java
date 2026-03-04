package FTP.Server;

/**
 * DTO con los datos de un usuario necesarios para autenticación (hash, perfil, estado).
 * Usado por UserStore para no exponer el User de dominio en la capa de persistencia.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class UserInfo {
    private final String username;
    private final String passwordHash;
    private final UserProfile profile;
    private final boolean enabled;

    public UserInfo(String username, String passwordHash, UserProfile profile, boolean enabled) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.profile = profile;
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
