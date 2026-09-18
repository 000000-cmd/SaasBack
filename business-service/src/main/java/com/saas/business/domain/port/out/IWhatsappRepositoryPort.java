package com.saas.business.domain.port.out;

import com.saas.business.domain.model.WhatsappSession;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo que la conversacion de WhatsApp necesita guardar: el mensaje que entro y
 * en que punto va cada charla.
 */
public interface IWhatsappRepositoryPort {

    /**
     * Deja el mensaje si su id de Meta no estaba. {@code false} = reentrega.
     *
     * <p>Es la deduplicacion que exige la especificacion, y va por indice unico
     * y no por un {@code if}: dos entregas simultaneas del mismo webhook
     * pasarian las dos cualquier comprobacion previa.</p>
     */
    boolean saveIfFirst(String waMessageId, UUID businessId, String fromPhone,
                        String toPhone, String body, String rawPayload);

    /** Marca el mensaje como interpretado; con {@code error} si no se pudo. */
    void markProcessed(String waMessageId, String error);

    Optional<WhatsappSession> session(UUID businessId, String phoneE164);

    WhatsappSession saveSession(WhatsappSession session);

    void dropSession(UUID businessId, String phoneE164);
}
