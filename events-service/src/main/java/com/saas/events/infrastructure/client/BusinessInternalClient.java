package com.saas.events.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Lo que este servicio necesita saber de un negocio para enviar en su nombre.
 *
 * <p>Vive en business-service, que es quien gobierna {@code saas_db}: aqui no
 * se puede leer esa base.</p>
 *
 * <p>Se pregunta en el momento de enviar y NO viaja en el evento de Kafka. Un
 * evento se guarda en el topic, se reintenta y se registra en los logs: un
 * token ahi dentro acaba escrito en sitios que nadie limpia.</p>
 */
@FeignClient(name = "business-service", contextId = "business-for-events", path = "/business")
public interface BusinessInternalClient {

    /** Nulos cuando el negocio no conecto su numero: entonces se usa el de la plataforma. */
    record WhatsappCredentials(String phoneNumberId, String accessToken) {}

    @GetMapping("/internal/whatsapp-credentials")
    WhatsappCredentials whatsappCredentials(@RequestParam("businessId") UUID businessId);
}
