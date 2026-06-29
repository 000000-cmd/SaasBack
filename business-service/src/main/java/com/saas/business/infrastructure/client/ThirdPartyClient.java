package com.saas.business.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Cliente S2S a thirdparty-service para dar de alta la persona (tercero) durante
 * el aprovisionamiento del negocio. Usa el endpoint interno {@code /internal/**}
 * (sin JWT), llamado directo via Eureka (no por el gateway).
 *
 * <p>{@code path = "/thirdparty"} = context-path del servicio.</p>
 */
@FeignClient(name = "thirdparty-service", contextId = "thirdparty-internal-business", path = "/thirdparty")
public interface ThirdPartyClient {

    @PostMapping("/internal/third-parties")
    PersonResponse createPerson(@RequestBody CreatePersonRequest request);

    /** Espejo de ThirdPartyRequest (solo los campos que el aprovisionamiento envia). */
    record CreatePersonRequest(
            UUID documentTypeId,
            String documentNumber,
            UUID userId,
            UUID businessId,
            String firstName,
            String secondName,
            String firstLastName,
            String secondLastName,
            UUID genderId,
            LocalDate birthDate,
            String photoUrl
    ) {}

    /** Solo se necesita el id del tercero creado. */
    record PersonResponse(UUID id) {}
}
