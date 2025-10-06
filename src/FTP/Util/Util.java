package FTP.Util;

/**
 * Clase de utilidades para la impresión de mensajes en consola con colores.
 * <p>
 * Se utilizan códigos ANSI para dar formato a los mensajes y diferenciarlos según el tipo
 * (por ejemplo, mensajes de error en rojo, mensajes de éxito en verde, etc.).
 * <p>
 * Esta clase es estática y sus métodos son de utilidad para imprimir mensajes en
 * diferentes colores, facilitando la visualización de la salida en consola.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class Util {
	/** Código ANSI para color rojo */
    private static final String ANSI_RED = "\u001B[31m";

	/** Código ANSI para color verde */
    private static final String ANSI_GREEN = "\u001B[32m";

	/** Código ANSI para color amarillo */
    private static final String ANSI_YELLOW = "\u001B[33m";

	/** Código ANSI para resetear color */
    private static final String ANSI_RESET = "\u001B[0m";

    /**
     * Imprime un mensaje en color rojo en la consola.
     * Útil para mensajes de error o advertencias críticas.
     *
     * @param message Mensaje a imprimir
     */
    public static void printRedColor(String message) {
        System.out.println(ANSI_RED + message + ANSI_RESET);
    }
    
    /**
     * Imprime un mensaje en color verde en la consola.
     * Útil para mensajes de éxito o confirmación.
     *
     * @param message Mensaje a imprimir
     */
    public static void printGreenColor(String message) {
        System.out.println(ANSI_GREEN + message + ANSI_RESET);
    }

    /**
     * Imprime un mensaje en color amarillo en la consola.
     * Útil para mensajes de advertencia o información importante.
     *
     * @param message Mensaje a imprimir
     */
    public static void printYellowColor(String message) {
        System.out.println(ANSI_YELLOW + message + ANSI_RESET);
    }

    /**
     * Imprime un mensaje de error en la salida de error estándar (System.err).
     * Fuerza el vaciado del buffer para asegurar que el mensaje aparezca
     * inmediatamente en la consola.
     *
     * @param messsage Mensaje de error a imprimir
     */
    public static void printErrorColor(String messsage) {
        System.err.println(messsage);
        System.err.flush();
    }
}
