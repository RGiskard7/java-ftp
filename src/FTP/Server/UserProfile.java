package FTP.Server;

/**
 * Enumeración de perfiles de usuario para control de acceso basado en roles (RBAC).
 * <p>
 * Define tres niveles de permisos:
 * <ul>
 *   <li><b>BASICO:</b> Solo lectura (LIST, RETR, CWD, PWD, CDUP)</li>
 *   <li><b>INTERMEDIO:</b> Lectura/escritura (añade STOR, DELE, MKD, RMD, RNFR, RNTO)</li>
 *   <li><b>ADMINISTRADOR:</b> Acceso completo a todos los comandos</li>
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
