package com.saas.common.util;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utilidades comunes para strings y códigos.
 */
public final class StringUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private StringUtils() {
        // Utility class
    }

    /**
     * Genera un código a partir de un nombre (slug)
     * Ejemplo: "Mi Nuevo Rol" -> "MI_NUEVO_ROL"
     */
    public static String toCode(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        String slug = WHITESPACE.matcher(withoutAccents).replaceAll("_");
        slug = NONLATIN.matcher(slug).replaceAll("");
        return slug.toUpperCase();
    }

    /**
     * Genera un UUID como string sin guiones
     */
    public static String generateCompactUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Verifica si un string es null o vacío
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isBlank();
    }

    /**
     * Verifica si un string no es null ni vacío
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Trunca un string a la longitud máxima especificada
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }

    /**
     * Capitaliza la primera letra de cada palabra
     */
    public static String toTitleCase(String input) {
        if (isEmpty(input)) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }
}
