package com.saas.events.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Acceso a los contactos de terceros, que viven en {@code saas_db} y gobierna
 * thirdparty-service. Este servicio no puede leer esa base: tiene la suya.
 *
 * El atributo {@code path} corresponde al context-path del downstream.
 */
@FeignClient(name = "thirdparty-service", contextId = "thirdparty-for-events", path = "/thirdparty")
public interface ThirdPartyInternalClient {

    record ContactTarget(UUID thirdPartyId, UUID contactId, String value) {}

    record ContactDetail(UUID id, UUID thirdPartyId, String value,
                         String typeCode, Boolean isVerified) {}

    /** Audiencia real de un lanzamiento global por este medio. */
    @GetMapping("/internal/contacts/primary-verified")
    List<ContactTarget> primaryVerified(@RequestParam("typeCode") String typeCode);

    /** Recuento previo, para que nadie lance a ciegas. */
    @GetMapping("/internal/contacts/count")
    Map<String, Long> countPrimaryVerified(@RequestParam("typeCode") String typeCode);

    @GetMapping("/internal/contacts/{id}")
    ContactDetail contact(@PathVariable("id") UUID id);

    /** Sella el contacto como verificado tras acertar el codigo. */
    @PutMapping("/internal/contacts/{id}/verify")
    ContactDetail verifyContact(@PathVariable("id") UUID id);

    /** Solo el id: es lo unico que necesita la bandeja para saber de quien es. */
    record ThirdPartyRef(UUID id) {}

    /**
     * El tercero de una cuenta de usuario. La bandeja se guarda por TERCERO
     * (porque no todo destinatario tiene cuenta), pero quien abre la campana se
     * identifica con su usuario: esta llamada es el puente.
     */
    @GetMapping("/internal/third-parties/by-user/{userId}")
    ThirdPartyRef byUser(@PathVariable("userId") UUID userId);
}
