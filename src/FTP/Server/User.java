package FTP.Server;

/**
 * Representa un usuario del sistema FTP con nombre y perfil de acceso.
 * <p>
 * Las contraseñas no se almacenan en memoria; la verificación se hace contra
 * el hash almacenado en el fichero de usuarios (bcrypt).
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class User {
	/** Nombre de usuario */
	private String username;

	/** Perfil de acceso del usuario (BASICO, INTERMEDIO, ADMINISTRADOR) */
	private UserProfile profile;

	/**
	 * Constructor por defecto.
	 */
	public User() {}

	/**
	 * Constructor con parámetros (sin contraseña; solo identidad y perfil).
	 *
	 * @param username Nombre de usuario
	 * @param profile Perfil de acceso
	 */
	public User(String username, UserProfile profile) {
		this.username = username;
		this.profile = profile;
	}

	/**
	 * Obtiene el nombre de usuario.
	 *
	 * @return Nombre de usuario
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Establece el nombre de usuario.
	 *
	 * @param username Nombre de usuario a establecer
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Obtiene el perfil de acceso del usuario.
	 *
	 * @return Perfil del usuario
	 */
	public UserProfile getProfile() {
		return profile;
	}

	/**
	 * Establece el perfil de acceso del usuario.
	 *
	 * @param profile Perfil a establecer
	 */
	public void setProfile(UserProfile profile) {
		this.profile = profile;
	}
}
