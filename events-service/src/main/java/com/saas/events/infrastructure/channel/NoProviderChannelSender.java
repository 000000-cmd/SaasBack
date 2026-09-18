package com.saas.events.infrastructure.channel;

import com.saas.events.domain.model.Attachment;
import com.saas.events.domain.model.ChannelType;
import com.saas.events.domain.model.NotificationSetting;
import com.saas.events.domain.model.SendStatus;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * SMS: se redacta y se previsualiza, pero todavia no hay proveedor. Se registra
 * el intento como NO_PROVIDER en lugar de fallar en silencio, para que en la
 * bitacora se vea que la notificacion se disparo y por que no salio.
 */
@Component
public class NoProviderChannelSender implements ChannelSender {

    /**
     * Solo SMS.
     *
     * <p>PUSH tiene su propio {@link PushChannelSender} y WHATSAPP su
     * {@link WhatsAppChannelSender} — que reclama el canal SIEMPRE y responde
     * NO_PROVIDER el mismo cuando le faltan credenciales. Si dos beans
     * declararan el mismo tipo, cual gana dependeria del orden en que Spring
     * inyecte la lista: justo el tipo de fallo que no se reproduce.</p>
     */
    @Override
    public boolean supports(ChannelType type) {
        return type == ChannelType.SMS;
    }

    @Override
    public Outcome send(NotificationSetting s, String recipient, String subject,
                        String body, Attachment attachment, UUID businessId) {
        return new Outcome(SendStatus.NO_PROVIDER, null,
                "Canal sin proveedor conectado todavía");
    }
}
