package FTP.Server;

/**
 * Abstracción para obtener datos de usuarios (fichero TXT o SQLite).
 * El servidor usa esto para USER/PASS sin conocer el backend.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public interface UserStore {

    /**
     * Busca un usuario por nombre. Si está desactivado (enabled=false), puede devolver null
     * o un UserInfo con enabled=false; el llamador debe rechazar login en ese caso.
     *
     * @param username Nombre de usuario
     * @return UserInfo con hash, profile y enabled, o null si no existe
     */
    UserInfo findByUsername(String username);
}
