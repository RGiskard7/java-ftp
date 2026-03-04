package FTP.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Almacén de usuarios desde fichero TXT (formato username:bcryptHash:profile).
 * Retrocompatibilidad cuando ftp.users.database no está configurado. Todos los usuarios se consideran enabled.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class FileUserStore implements UserStore {

    private final String filePath;

    public FileUserStore(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public UserInfo findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(":", 3);
                if (parts.length != 3) {
                    FTP.Util.FileLogger.warning("Línea de usuario mal formada (se esperan 3 campos): " + line);
                    continue;
                }
                String u = parts[0].trim();
                String hash = parts[1].trim();
                String profileStr = parts[2].trim();
                if (!u.equals(username.trim())) {
                    continue;
                }
                UserProfile profile;
                try {
                    profile = UserProfile.valueOf(profileStr);
                } catch (IllegalArgumentException e) {
                    FTP.Util.FileLogger.warning("Perfil de usuario no válido: " + profileStr);
                    return null;
                }
                return new UserInfo(u, hash, profile, true);
            }
        } catch (IOException e) {
            FTP.Util.FileLogger.error("Error leyendo fichero de usuarios: " + e.getMessage());
        }
        return null;
    }
}
