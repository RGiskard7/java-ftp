package FTP.Server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Limita intentos de login fallidos por IP para mitigar fuerza bruta.
 * Mantiene una ventana temporal de intentos; si se supera el límite, la IP queda bloqueada durante un tiempo.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class LoginThrottle {
    private final int maxAttempts;
    private final long lockoutMillis;
    private final Map<String, List<Long>> failuresByIp = new ConcurrentHashMap<>();

    public LoginThrottle(int maxAttempts, int lockoutMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockoutMillis = lockoutMinutes <= 0 ? 0 : lockoutMinutes * 60L * 1000L;
    }

    /**
     * Indica si la IP está bloqueada por exceso de intentos fallidos.
     *
     * @param ip Dirección IP del cliente
     * @return true si debe rechazarse el intento de login (bloqueado)
     */
    public boolean isBlocked(String ip) {
        if (lockoutMillis <= 0 || maxAttempts <= 0) return false;
        List<Long> times = failuresByIp.get(ip);
        if (times == null) return false;
        long now = System.currentTimeMillis();
        times.removeIf(t -> now - t > lockoutMillis);
        if (times.isEmpty()) {
            failuresByIp.remove(ip);
            return false;
        }
        return times.size() >= maxAttempts;
    }

    /**
     * Registra un intento de login fallido para la IP.
     *
     * @param ip Dirección IP del cliente
     */
    public void recordFailure(String ip) {
        if (lockoutMillis <= 0 || maxAttempts <= 0) return;
        failuresByIp.computeIfAbsent(ip, k -> new CopyOnWriteArrayList<>()).add(System.currentTimeMillis());
    }

    /**
     * Limpia el historial de fallos para la IP (p. ej. tras un login exitoso).
     *
     * @param ip Dirección IP del cliente
     */
    public void clear(String ip) {
        failuresByIp.remove(ip);
    }
}
