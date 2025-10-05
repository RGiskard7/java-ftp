package FTP.Util;

/**
 * Clase de utilidades para la impresión de mensajes en consola con colores.
 * 
 * Se utilizan códigos ANSI para dar formato a los mensajes y diferenciarlos según el tipo
 * (por ejemplo, mensajes de error en rojo, mensajes de éxito en verde, etc.).
 * 
 * Nota: Esta clase es estática y sus métodos son de utilidad para imprimir mensajes en
 * diferentes colores, facilitando la visualización de la salida en consola.
 * 
 * @author Eduardo Díaz Sánchez
 */
public class Util {
	// Códigos ANSI para establecer colores en la consola
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RESET = "\u001B[0m";
    
    /**
     * Imprime el mensaje proporcionado en color rojo en la consola.
     * 
     * @param message El mensaje a imprimir.
     */
    public static void printRedColor(String message) {
        System.out.println(ANSI_RED + message + ANSI_RESET);
    }
    
    /**
     * Imprime el mensaje proporcionado en color verde en la consola.
     * 
     * @param message El mensaje a imprimir.
     */
    public static void printGreenColor(String message) {
        System.out.println(ANSI_GREEN + message + ANSI_RESET);
    }
    
    /**
     * Imprime el mensaje proporcionado en color amarillo en la consola.
     * 
     * @param message El mensaje a imprimir.
     */
    public static void printYellowColor(String message) {
        System.out.println(ANSI_YELLOW + message + ANSI_RESET);
    }
	
    /**
     * Imprime el mensaje de error en la salida de error (System.err) 
     * y fuerza el vaciado del buffer con la intención de que salga 
     * correctamente en la consola (a veces la salida de err no sale 
     * en el orden correcto).
     * 
     * @param messsage El mensaje de error a imprimir.
     */
    public static void printErrorColor(String messsage) {
        System.err.println(messsage);
        System.err.flush();
    }
}
