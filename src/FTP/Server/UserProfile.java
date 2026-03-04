package FTP.Server;

/**
 * Enumeración de perfiles de usuario para control de acceso basado en roles (RBAC).
 * <p>
 * Define tres niveles de permisos:
 * <ul>
 *   <li><b>BASICO:</b> Solo lectura (LIST, RETR, SIZE, MDTM, CWD, CDUP, PWD)</li>
 *   <li><b>INTERMEDIO:</b> Lectura/escritura (añade STOR, DELE)</li>
 *   <li><b>ADMINISTRADOR:</b> Acceso completo (añade MKD, RMD, RNFR, RNTO)</li>
 * </ul>
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public enum UserProfile {
	/** Perfil básico con permisos de solo lectura */
	BASICO,

	/** Perfil intermedio con permisos de lectura y escritura */
	INTERMEDIO,

	/** Perfil administrador con acceso completo */
	ADMINISTRADOR
}
