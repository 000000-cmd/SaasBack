package com.saas.events.infrastructure.channel;

import com.saas.events.domain.model.Attachment;
import com.saas.events.domain.model.ChannelType;
import com.saas.events.domain.model.NotificationSetting;
import com.saas.events.domain.model.SendStatus;
import com.saas.events.domain.model.UserDevice;
import com.saas.events.domain.port.out.IUserDeviceRepositoryPort;
import com.saas.events.infrastructure.client.FcmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Notificacion del sistema operativo en el telefono, via FCM.
 *
 * A diferencia de los demas canales, el "destinatario" que llega aqui NO es una
 * direccion: es el id del tercero. Sus dispositivos se resuelven en este punto,
 * porque un token no es un dato de contacto que nadie escriba a mano — lo
 * registra la app al arrancar.
 *
 * Mientras no haya credencial de FCM, registra NO_PROVIDER con su motivo, igual
 * que SMS y WhatsApp. La bandeja se escribe igual, asi que la notificacion
 * existe y se ve en la web y en el movil: simplemente no suena.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushChannelSender implements ChannelSender {

    private final IUserDeviceRepositoryPort devices;
    private final FcmClient fcm;

    @Override
    public boolean supports(ChannelType type) { return type == ChannelType.PUSH; }

    @Override
    public Outcome send(NotificationSetting settings, String recipient, String subject,
                        String body, Attachment attachment, UUID businessId) {
        // Una push no lleva ficheros: el adjunto se ignora a proposito. El
        // extracto viaja por correo y ademas vive en la app.
        UUID owner = parseOwner(recipient);
        if (owner == null) {
            return new Outcome(SendStatus.SKIPPED, null,
                    "Push necesita el id del tercero como destinatario, llegó: " + recipient);
        }

        List<UserDevice> targets = devices.findByOwner(owner).stream()
                .filter(d -> Boolean.TRUE.equals(d.getEnabled()))
                .toList();
        if (targets.isEmpty()) {
            return new Outcome(SendStatus.SKIPPED, null, "Sin dispositivos registrados");
        }

        if (!fcm.isConfigured()) {
            return new Outcome(SendStatus.NO_PROVIDER, null,
                    "FCM sin credenciales: " + targets.size() + " dispositivo(s) en espera");
        }

        int enviados = 0;
        String ultimoId = null;
        String ultimoError = null;
        for (UserDevice d : targets) {
            FcmClient.PushResult r = fcm.send(d.getFcmToken(), subject, body);
            if (r.ok()) {
                enviados++;
                ultimoId = r.messageId();
            } else {
                ultimoError = r.error();
                // Un token que el proveedor declara invalido se borra. Si no, la
                // lista se llena de fantasmas y cada envio gasta una llamada por
                // cada uno, para siempre.
                if (r.tokenInvalid()) {
                    log.info("Token invalido, se elimina el dispositivo: {}", d.getId());
                    devices.deleteByToken(d.getFcmToken());
                }
            }
        }

        return enviados > 0
                ? new Outcome(SendStatus.SENT, ultimoId, null)
                : new Outcome(SendStatus.FAILED, null, ultimoError);
    }

    private static UUID parseOwner(String recipient) {
        try {
            return UUID.fromString(recipient.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
