package com.saas.events.infrastructure.channel;

import com.saas.events.domain.model.Attachment;
import com.saas.events.domain.model.ChannelType;
import com.saas.events.domain.model.NotificationSetting;
import com.saas.events.domain.model.SendStatus;
import java.util.UUID;

/**
 * Un canal de salida. Mismo patron que EventHandler en search-service: Spring
 * inyecta todos los beans que implementan esta interfaz y el despachador elige
 * el primero que soporta el tipo. Anadir WhatsApp o SMS reales sera crear una
 * clase mas; nada del despachador cambia.
 */
public interface ChannelSender {

    boolean supports(ChannelType type);

    /**
     * @param attachment adjunto opcional (puede ser null). Un canal que no sepa
     *        transportarlo simplemente lo ignora: es preferible que el mensaje
     *        llegue sin el papel a que no llegue.
     */
    /**
     * @param businessId de quien sale el mensaje. Lo usa WhatsApp para enviar
     *        con el numero del negocio en vez de con el de la plataforma; los
     *        demas canales lo ignoran. Va {@code null} cuando el aviso no es de
     *        ningun negocio en concreto (un lanzamiento global, por ejemplo).
     */
    Outcome send(NotificationSetting settings, String recipient, String subject,
                 String body, Attachment attachment, java.util.UUID businessId);

    record Outcome(SendStatus status, String providerMessageId, String error) {}
}
