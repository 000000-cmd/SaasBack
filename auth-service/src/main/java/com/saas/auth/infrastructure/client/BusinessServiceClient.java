package com.saas.auth.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign a business-service (lb://business-service) para resolver el businessId
 * del dueño al emitir el token. Solo endpoints {@code /internal/**} (S2S, no
 * traversed por el gateway). {@code path = "/business"} = context-path del
 * servicio. El endpoint interno devuelve el DTO crudo (convención /internal).
 */
@FeignClient(name = "business-service", contextId = "business-service-internal", path = "/business")
public interface BusinessServiceClient {

    @GetMapping("/internal/owner-business")
    OwnerBusinessDto ownerBusiness(@RequestParam("userId") UUID userId);

    /** Espejo de business.OwnerBusinessResponse (solo el id del negocio). */
    record OwnerBusinessDto(UUID businessId) {}
}
