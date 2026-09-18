package com.saas.business.domain.model;

import java.security.SecureRandom;

/**
 * El codigo con el que un cliente sin cuenta consulta o cancela su cita.
 *
 * <p><b>No es secuencial.</b> Si lo fuera, quien reserve una vez podria
 * recorrer las citas de todo el negocio sumando uno. Sale de
 * {@link SecureRandom}, no de un contador.</p>
 *
 * <p><b>El alfabeto es el de Crockford</b> (base 32 sin {@code I}, {@code L},
 * {@code O} ni {@code U}). Este codigo se dicta por telefono y se copia a mano
 * de un mensaje, y esas cuatro letras son las que se confunden: la I con el 1,
 * la O con el 0, la L con el 1, y la U que suena como la V. Al no existir en el
 * codigo, quien las teclee esta escribiendo otra cosa — y
 * {@link #normalize(String)} sabe cual.</p>
 *
 * <p>Ocho caracteres sobre 32 son 1,1 · 10^12 combinaciones. Probar codigos al
 * azar no lleva a ningun sitio, y aun asi la pantalla publica pide ademas los
 * ultimos cuatro digitos del telefono: el codigo dice cual es la cita, el
 * telefono dice que es tuya.</p>
 */
public final class PublicCode {

    private static final char[] ALFABETO = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final String ALFABETO_STR = new String(ALFABETO);
    private static final int LARGO = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicCode() { }

    public static String generate() {
        StringBuilder sb = new StringBuilder(LARGO);
        for (int i = 0; i < LARGO; i++) {
            sb.append(ALFABETO[RANDOM.nextInt(ALFABETO.length)]);
        }
        return sb.toString();
    }

    /**
     * Normaliza lo que escriba el cliente antes de buscarlo.
     *
     * <p>Mayusculas, sin espacios ni guiones, y las cuatro letras excluidas
     * traducidas al caracter que de verdad representan. Sin esto, un codigo
     * bien dictado y mal tecleado devuelve "no existe", que es la respuesta mas
     * inutil que se le puede dar a alguien que si tiene una cita.</p>
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase().replaceAll("[\\s\\-_.]+", "");
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            out.append(switch (c) {
                case 'I', 'L' -> '1';
                case 'O' -> '0';
                // La U no se traduce: no se parece a ningun caracter valido,
                // asi que un codigo con U esta mal y debe fallar, no adivinarse.
                default -> c;
            });
        }
        return out.toString();
    }

    /** El texto tiene la forma de un codigo publico. */
    public static boolean looksValid(String code) {
        if (code == null || code.length() != LARGO) return false;
        for (char c : code.toCharArray()) {
            if (ALFABETO_STR.indexOf(c) < 0) return false;
        }
        return true;
    }

    /**
     * Comprobacion ejecutable.
     *
     * <p>Cubre lo que de verdad pasa: alguien copia el codigo con un espacio,
     * escribe la O creyendo que es un cero, o teclea una ele por un uno.</p>
     */
    public static void check() {
        String generado = generate();
        if (!looksValid(generado)) {
            throw new IllegalStateException("El codigo generado no se valida a si mismo: " + generado);
        }
        // Espacios, guion, minusculas, y la O que alguien tecleo creyendo ver
        // un cero: todo tiene que acabar en el mismo codigo.
        if (!"01Z3ABCD".equals(normalize(" o1z3-abcd "))) {
            throw new IllegalStateException("normalize no limpia lo que debe: " + normalize(" o1z3-abcd "));
        }
        if (!"11".equals(normalize("IL"))) {
            throw new IllegalStateException("La I y la L tienen que volverse unos");
        }
        if (looksValid("ABCDEFGU")) {
            throw new IllegalStateException("La U no pertenece al alfabeto");
        }
        if (looksValid("ABC")) {
            throw new IllegalStateException("Un codigo corto no puede darse por bueno");
        }
        // Dos codigos seguidos no pueden salir iguales: seria un contador
        // disfrazado.
        if (generate().equals(generate())) {
            throw new IllegalStateException("Dos codigos consecutivos coincidieron");
        }
    }
}
