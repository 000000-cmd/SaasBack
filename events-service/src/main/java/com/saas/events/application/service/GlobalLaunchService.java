package com.saas.events.application.service;

import com.saas.events.domain.model.ChannelType;
import com.saas.events.domain.model.Notification;
import com.saas.events.domain.port.out.INotificationRepositoryPort;
import com.saas.events.infrastructure.client.ThirdPartyInternalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lanzamiento de una notificacion a toda la base.
 *
 * Solo se pueden lanzar las notificaciones marcadas {@code IsGlobal}: es un
 * interruptor por registro, no un permiso general, porque escribirle a todos los
 * clientes de golpe es lo mas irreversible que ofrece el panel.
 *
 * La audiencia es el contacto PRINCIPAL Y VERIFICADO de cada tercero para el
 * medio elegido. Quien no lo tenga no recibe — y queda constancia del motivo,
 * que es la diferencia entre "no le llego" y "no sabemos".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalLaunchService {

    /** Canal de envio -> tipo de contacto donde vive su direccion. */
    private static final Map<ChannelType, String> CONTACTO_POR_CANAL = Map.of(
            ChannelType.EMAIL, "EMAIL",
            ChannelType.SMS, "MOBILE",
            ChannelType.WHATSAPP, "WHATSAPP");

    private final INotificationRepositoryPort notifications;
    private final ThirdPartyInternalClient thirdParty;
    private final DispatchService dispatch;

    /** Notificaciones que se pueden lanzar. Es lo unico que lista la pantalla. */
    public List<Notification> launchable() {
        return notifications.findAll().stream()
                .filter(n -> Boolean.TRUE.equals(n.getIsGlobal()))
                .filter(n -> Boolean.TRUE.equals(n.getEnabled()))
                .toList();
    }

    /**
     * Cuantos recibirian por cada medio, ANTES de lanzar. Un boton de un clic que
     * escribe a toda la base es un accidente esperando ocurrir; esto es lo que
     * convierte la decision en informada.
     *
     * PUSH no se cuenta por contacto: su destino son dispositivos registrados, no
     * una direccion que alguien escriba. Se informa aparte para no mentir con un 0.
     */
    public Map<String, Long> audience(List<String> channelCodes) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (String code : channelCodes) {
            ChannelType channel = ChannelType.from(code);
            if (channel == null) continue;
            String contactType = CONTACTO_POR_CANAL.get(channel);
            if (contactType == null) continue;   // PUSH
            try {
                Long n = thirdParty.countPrimaryVerified(contactType).get("count");
                out.put(channel.name(), n == null ? 0L : n);
            } catch (Exception ex) {
                log.warn("No se pudo contar audiencia de {}: {}", channel, ex.getMessage());
                out.put(channel.name(), -1L);   // -1 = no se pudo consultar, distinto de 0
            }
        }
        return out;
    }

    public record LaunchResult(String notificationCode, int recipients, Map<String, Long> byChannel) {}

    /**
     * Lanza. Recorre los contactos principales verificados de cada medio y
     * despacha uno por uno, escribiendo bandeja para cada destinatario.
     *
     * ponytail: recorrido sincrono. Aguanta de sobra el tamano actual de la base;
     * si algun dia son decenas de miles, el sitio de la cola es aqui — publicar
     * un evento por destinatario al outbox y dejar que el listener los consuma.
     */
    public LaunchResult launch(String notificationCode, List<String> channelCodes) {
        Notification n = notifications.findByCode(notificationCode)
                .filter(x -> Boolean.TRUE.equals(x.getIsGlobal()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "La notificación no existe o no está marcada como global"));

        Map<String, Long> porCanal = new LinkedHashMap<>();
        List<String> yaEnviados = new ArrayList<>();
        int total = 0;

        for (String code : channelCodes) {
            ChannelType channel = ChannelType.from(code);
            String contactType = channel == null ? null : CONTACTO_POR_CANAL.get(channel);
            if (contactType == null) continue;

            List<ThirdPartyInternalClient.ContactTarget> destinos =
                    thirdParty.primaryVerified(contactType);
            long enviados = 0;
            for (ThirdPartyInternalClient.ContactTarget d : destinos) {
                // Una persona con el mismo valor en dos medios no recibe dos veces
                // lo mismo por el mismo canal.
                String clave = channel.name() + "|" + d.value();
                if (yaEnviados.contains(clave)) continue;
                yaEnviados.add(clave);

                dispatch.dispatch(n.getCode(), List.of(d.value()), Map.of(), null, d.thirdPartyId());
                enviados++;
                total++;
            }
            porCanal.put(channel.name(), enviados);
        }

        log.info("Lanzamiento global '{}': {} destinatarios {}", n.getCode(), total, porCanal);
        return new LaunchResult(n.getCode(), total, porCanal);
    }
}
