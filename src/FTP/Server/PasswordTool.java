package FTP.Server;

import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Herramienta de línea de comandos para gestionar usuarios del servidor FTP.
 * Genera contraseñas hasheadas con bcrypt y permite añadir líneas al fichero de usuarios.
 * <p>
 * Uso:
 * <pre>
 *   java -cp "bin:lib/commons-net-3.11.1.jar:lib/jbcrypt-0.4.jar" FTP.Server.PasswordTool adduser &lt;username&gt; &lt;password&gt; &lt;BASICO|INTERMEDIO|ADMINISTRADOR&gt; [users.txt]
 *   java ... FTP.Server.PasswordTool hash &lt;password&gt;
 * </pre>
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class PasswordTool {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        String cmd = args[0].toLowerCase();
        if ("adduser".equals(cmd)) {
            if (args.length < 4) {
                System.err.println("Uso: adduser <username> <password> <BASICO|INTERMEDIO|ADMINISTRADOR> [ruta-users.txt]");
                return;
            }
            String username = args[1];
            String password = args[2];
            String profileStr = args[3].toUpperCase();
            String usersPath = args.length > 4 ? args[4] : "files/users/users.txt";
            addUser(username, password, profileStr, usersPath);
        } else if ("hash".equals(cmd)) {
            if (args.length < 2) {
                System.err.println("Uso: hash <password>");
                return;
            }
            System.out.println(BCrypt.hashpw(args[1], BCrypt.gensalt()));
        } else {
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("PasswordTool - Gestión de usuarios FTP (bcrypt)");
        System.out.println();
        System.out.println("  adduser <username> <password> <BASICO|INTERMEDIO|ADMINISTRADOR> [ruta-users.txt]");
        System.out.println("    Añade un usuario al fichero de usuarios con contraseña hasheada.");
        System.out.println();
        System.out.println("  hash <password>");
        System.out.println("    Imprime el hash bcrypt de la contraseña (para uso manual).");
    }

    private static void addUser(String username, String password, String profileStr, String usersPath) {
        UserProfile profile;
        try {
            profile = UserProfile.valueOf(profileStr);
        } catch (IllegalArgumentException e) {
            System.err.println("Perfil no válido. Use: BASICO, INTERMEDIO o ADMINISTRADOR");
            return;
        }
        if (username.contains(":") || username.trim().isEmpty()) {
            System.err.println("El nombre de usuario no puede estar vacío ni contener ':'.");
            return;
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String line = username + ":" + hash + ":" + profile.name();

        Path path = Paths.get(usersPath);
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.write(path, (line + "\n").getBytes(), StandardOpenOption.CREATE_NEW);
            } else {
                List<String> lines = new ArrayList<>(Files.readAllLines(path));
                boolean found = false;
                for (int i = 0; i < lines.size(); i++) {
                    String l = lines.get(i).trim();
                    if (l.isEmpty() || l.startsWith("#")) continue;
                    String[] parts = l.split(":", 3);
                    if (parts.length >= 1 && parts[0].trim().equals(username)) {
                        lines.set(i, line);
                        found = true;
                        break;
                    }
                }
                if (!found) lines.add(line);
                Files.write(path, lines);
            }
            System.out.println("Usuario '" + username + "' añadido/actualizado en " + path.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error escribiendo " + usersPath + ": " + e.getMessage());
        }
    }
}
