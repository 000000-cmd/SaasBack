package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un mensaje entrante, tal y como llego.
 *
 * <p>Se guarda el payload CRUDO antes de interpretarlo: cuando una conversacion
 * se tuerce, es lo unico que dice que mando Meta de verdad. El objeto ya
 * interpretado solo dice lo que entendimos.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class WhatsappMessage extends BaseDomain {
    /** El id que pone Meta. Es la deduplicacion: los webhooks se reentregan. */
    private String waMessageId;
    private UUID businessId;
    private String fromPhone;
    private String toPhone;
    private String body;
    private String rawPayload;
    private LocalDateTime receivedAt;
    /** Nulo = todavia no se pudo interpretar. Se deja para poder reintentar. */
    private LocalDateTime processedAt;
    private String error;
}
