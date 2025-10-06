package FTP.Server;

/**
 * Representa un usuario del sistema FTP con sus credenciales y perfil de acceso.
 * <p>
 * Cada usuario tiene un nombre de usuario, contraseña y perfil que determina
 * los permisos de acceso a comandos FTP.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class User {
	/** Nombre de usuario */
	private String username;

	/** Contraseña del usuario */
	private String password;

	/** Perfil de acceso del usuario (BASICO, INTERMEDIO, ADMINISTRADOR) */
	private UserProfile profile;

	/**
	 * Constructor por defecto.
	 */
	public User() {}

	/**
	 * Constructor con parámetros.
	 *
	 * @param username Nombre de usuario
	 * @param password Contraseña
	 * @param profile Perfil de acceso
	 */
	public User(String username, String password, UserProfile profile) {
		this.username = username;
		this.password = password;
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
	 * Obtiene la contraseña del usuario.
	 *
	 * @return Contraseña del usuario
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Establece la contraseña del usuario.
	 *
	 * @param password Contraseña a establecer
	 */
	public void setPassword(String password) {
		this.password = password;
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

	/**
	 * Compara dos contraseñas para verificar autenticación.
	 *
	 * @param userPassword Contraseña almacenada del usuario
	 * @param inputPassord Contraseña ingresada
	 * @return 0 si son iguales, valor negativo si userPassword &lt; inputPassord, positivo si userPassword &gt; inputPassord
	 */
	public static int checkPassword(String userPassword, String inputPassord) {
		return userPassword.compareTo(inputPassord);
	}

}
