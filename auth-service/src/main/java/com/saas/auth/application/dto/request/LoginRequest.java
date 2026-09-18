package com.saas.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de acceso.
 *
 * {@code surface} dice DESDE DÓNDE se intenta entrar. Las dos entradas de la web
 * están separadas de forma estricta:
 *
 *   ADMIN   (:4201) — solo administradores del sistema.
 *   PRODUCT (:4200) — todos los demás; un administrador NO entra por aquí.
 *
 * Se valida en el SERVIDOR y no solo en el navegador, porque el front se puede
 * saltar llamando a la API directamente. Cuando no cuadra, la respuesta es
 * exactamente la misma que la de un usuario inexistente: no se confirma que la
 * cuenta exista, ni mucho menos que sea de administrador.
 *
 * Es opcional para no romper a quien ya consume este endpoint (el APK, que es
 * otra superficie con sus propias reglas): ausente = sin restricción de origen.
 */
public record LoginRequest(
        @NotBlank String usernameOrEmail,
        @NotBlank String password,
        String surface,
        /**
         * De que aparato viene. Solo lo manda el APK; la web lo deja nulo y ahi
         * no hay regla de un dispositivo por cuenta — un dueno abre el panel en
         * el portatil y en el escritorio, y eso es normal.
         */
        DeviceInfo device,
        /**
         * El reintento despues de que la persona haya visto el aviso y haya
         * dicho que si. Sin esto, un choque de dispositivos responde 409 con el
         * detalle en vez de entrar.
         */
        boolean unlinkOthers
) {
    public static final String SURFACE_ADMIN = "ADMIN";
    public static final String SURFACE_PRODUCT = "PRODUCT";

    /** Para los llamadores internos que no tienen superficie (p. ej. el registro). */
    public LoginRequest(String usernameOrEmail, String password) {
        this(usernameOrEmail, password, null, null, false);
    }

    /** Login web: superficie, sin aparato. */
    public LoginRequest(String usernameOrEmail, String password, String surface) {
        this(usernameOrEmail, password, surface, null, false);
    }
}
