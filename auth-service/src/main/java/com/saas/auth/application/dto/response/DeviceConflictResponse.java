package com.saas.auth.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lo que se le cuenta a quien intenta entrar y ya tiene la sesion abierta en
 * otro sitio.
 *
 * <p>Es informacion para DECIDIR, no un error. Por eso lleva de que aparato se
 * trata y cuando se uso por ultima vez: sin eso, "tu cuenta esta abierta en
 * otro dispositivo" no le dice a nadie si es su telefono viejo o alguien mas.</p>
 */
public record DeviceConflictResponse(
        List<Conflict> conflicts,
        /** Copiado tal cual en el reintento con {@code unlinkOthers = true}. */
        String message
) {
    /**
     * @param kind        OTHER_DEVICE (la misma cuenta esta abierta en otro
     *                    aparato) u OTHER_ACCOUNT (en este aparato hay otra
     *                    cuenta abierta).
     * @param account     como se muestra la cuenta implicada. En OTHER_ACCOUNT
     *                    va PARCIALMENTE OCULTA: quien esta intentando entrar no
     *                    tiene por que enterarse del usuario completo de otro.
     */
    public record Conflict(
            UUID linkId,
            String kind,
            String deviceName,
            String platform,
            String account,
            LocalDateTime lastSeenAt
    ) {
        public static final String OTHER_DEVICE = "OTHER_DEVICE";
        public static final String OTHER_ACCOUNT = "OTHER_ACCOUNT";
    }
}
