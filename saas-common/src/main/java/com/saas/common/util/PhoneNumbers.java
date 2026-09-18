package com.saas.common.util;

/**
 * Normalizacion de telefonos a E.164, EN UN SOLO SITIO.
 *
 * <p>E.164 es el formato internacional: {@code +} seguido del indicativo de
 * pais y el numero, sin espacios ni guiones. {@code +573001234567}.</p>
 *
 * <h3>Por que hace falta</h3>
 * <p>Hasta ahora los telefonos se guardaban tal y como los escribia cada
 * pantalla: "300 123 4567" con espacios desde la web, quizas "3001234567"
 * desde el APK, y "+57 300 123 4567" si alguien lo pegaba de sus contactos.
 * Son tres cadenas distintas para el mismo telefono. Mientras solo servian
 * para mostrarlos daba igual; en el momento en que el telefono se convierte en
 * la IDENTIDAD del cliente —que es lo que hace la agenda— tres formas del
 * mismo numero son tres clientes distintos, con tres historiales partidos.</p>
 *
 * <h3>Por que a mano y no con libphonenumber</h3>
 * <p>La libreria de Google son ~7 MB de metadatos de 200 paises para resolver
 * un caso: Colombia, celulares de 10 digitos que empiezan por 3, y fijos de 10
 * con indicativo. Cuando haya un segundo pais en juego, esta clase se cambia
 * por la libreria y solo hay un sitio que tocar — que es justo el motivo de
 * que exista.</p>
 */
public final class PhoneNumbers {

    private PhoneNumbers() { }

    /** Indicativo por defecto cuando el numero llega sin el. */
    public static final String DEFAULT_COUNTRY_CODE = "57";

    /**
     * Deja el telefono en E.164, o devuelve {@code null} si no se puede.
     *
     * <p>Devolver null y no la cadena original es deliberado: un telefono que
     * no se puede normalizar NO sirve como identidad, y guardarlo "como venga"
     * es exactamente lo que crea los duplicados. Quien llama decide si eso es
     * un error de validacion o un campo que se queda vacio.</p>
     */
    public static String toE164(String raw) {
        return toE164(raw, DEFAULT_COUNTRY_CODE);
    }

    public static String toE164(String raw, String defaultCountryCode) {
        if (raw == null) return null;

        String limpio = raw.trim();
        if (limpio.isEmpty()) return null;

        // Un "+" al principio significa que el indicativo ya viene puesto.
        boolean traeIndicativo = limpio.startsWith("+");
        String digitos = limpio.replaceAll("\\D+", "");
        if (digitos.isEmpty()) return null;

        // "00" delante es la otra forma de escribir el "+" (marcacion
        // internacional a la europea). Cuenta igual.
        if (!traeIndicativo && digitos.startsWith("00")) {
            traeIndicativo = true;
            digitos = digitos.substring(2);
        }

        if (traeIndicativo) {
            // Ya viene completo: solo se comprueba que tenga un largo creible.
            // El rango de E.164 son 8 a 15 digitos contando el indicativo.
            return (digitos.length() >= 8 && digitos.length() <= 15) ? "+" + digitos : null;
        }

        // Sin "+": puede venir con el indicativo pegado igualmente
        // ("573001234567"), que es como lo devuelve WhatsApp.
        String cc = defaultCountryCode == null ? DEFAULT_COUNTRY_CODE : defaultCountryCode;
        if (digitos.length() > 10 && digitos.startsWith(cc)) {
            digitos = digitos.substring(cc.length());
        }

        // Numero nacional colombiano: exactamente 10 digitos, celular o fijo.
        if (digitos.length() != 10) return null;
        return "+" + cc + digitos;
    }

    /** {@code true} si el texto representa un telefono normalizable. */
    public static boolean isValid(String raw) {
        return toE164(raw) != null;
    }

    /**
     * De E.164 a como se lee en Colombia: {@code +573001234567 → 300 123 4567}.
     *
     * <p>Coincide con la mascara {@code phone} del estandar del front y del
     * APK, para que lo que se guarda y lo que se ve sean el mismo numero
     * escrito de las dos maneras y no dos numeros parecidos.</p>
     */
    public static String toLocalDisplay(String e164) {
        if (e164 == null) return null;
        String d = e164.replaceAll("\\D+", "");
        if (d.startsWith(DEFAULT_COUNTRY_CODE) && d.length() == 12) {
            d = d.substring(2);
        }
        if (d.length() != 10) return e164;
        return d.substring(0, 3) + " " + d.substring(3, 6) + " " + d.substring(6);
    }

    /**
     * Los ultimos cuatro digitos. Es lo que se le pide a un cliente sin cuenta
     * para probar que la cita es suya, junto con su codigo publico: pedirle el
     * telefono entero por una URL publica seria regalarselo a quien pruebe
     * codigos.
     */
    public static String last4(String e164) {
        if (e164 == null) return null;
        String d = e164.replaceAll("\\D+", "");
        return d.length() < 4 ? null : d.substring(d.length() - 4);
    }

    /**
     * Comprobacion ejecutable. Cubre las formas en que el mismo telefono llega
     * hoy desde la web, el APK y WhatsApp: todas tienen que acabar igual.
     */
    public static void check() {
        String esperado = "+573001234567";
        String[] mismasFormas = {
                "300 123 4567",      // como lo guarda hoy la web
                "3001234567",        // como lo manda el APK
                "+57 300 123 4567",  // pegado desde los contactos
                "573001234567",      // como lo devuelve WhatsApp
                "0057 300 1234567",  // marcacion internacional
                "  +573001234567 ",  // con espacios de sobra
        };
        for (String forma : mismasFormas) {
            String salida = toE164(forma);
            if (!esperado.equals(salida)) {
                throw new IllegalStateException(
                        "'" + forma + "' deberia normalizar a " + esperado + " y dio " + salida);
            }
        }

        // Lo que NO se puede normalizar devuelve null, nunca la cadena original.
        String[] invalidos = {null, "", "   ", "123", "abc", "30012345678", "+1"};
        for (String malo : invalidos) {
            if (toE164(malo) != null) {
                throw new IllegalStateException("'" + malo + "' no deberia normalizar");
            }
        }

        if (!"300 123 4567".equals(toLocalDisplay(esperado))) {
            throw new IllegalStateException("toLocalDisplay no coincide con la mascara 'phone'");
        }
        if (!"4567".equals(last4(esperado))) {
            throw new IllegalStateException("last4 incorrecto");
        }
        // Idempotencia: normalizar lo ya normalizado no puede cambiarlo.
        if (!esperado.equals(toE164(esperado))) {
            throw new IllegalStateException("toE164 no es idempotente");
        }
    }
}
