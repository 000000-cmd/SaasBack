package com.saas.events.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Codigo de un solo uso para verificar que un contacto es de quien dice serlo.
 *
 * Se guarda el HASH, nunca el codigo: es un secreto efimero y la tabla es tan
 * legible como cualquier otra para quien tenga acceso a la base.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class VerificationCode extends BaseDomain {

    /** El destino que se verifica: correo o telefono, tal cual. */
    private String target;
    /** Canal por el que salio: EMAIL, SMS, WHATSAPP. */
    private String channelCode;
    /** Para que se pidio. Hoy solo CONTACT_VERIFY. */
    private String purpose;
    private String codeHash;
    private LocalDateTime expiresAt;
    private Integer attempts;
    private Integer maxAttempts;
    /** NULL = todavia utilizable. Se marca al acertar o al agotar intentos. */
    private LocalDateTime consumedAt;
    /** Contacto a marcar como verificado al acertar. Vive en saas_db. */
    private UUID contactId;

    public boolean isConsumed() { return consumedAt != null; }
    public boolean isExpired()  { return expiresAt != null && expiresAt.isBefore(LocalDateTime.now()); }
    public boolean hasAttemptsLeft() {
        return attempts != null && maxAttempts != null && attempts < maxAttempts;
    }
    /** Utilizable = ni consumido, ni vencido, y con intentos disponibles. */
    public boolean isUsable() { return !isConsumed() && !isExpired() && hasAttemptsLeft(); }
}
