package com.saas.auth.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign a thirdparty-service (lb://thirdparty-service) para el login flexible:
 * resuelve la cuenta (userId) duena de un numero de documento. Solo endpoints
 * {@code /internal/**} (S2S, no atraviesan el gateway). {@code path} =
 * context-path del servicio.
 */
@FeignClient(name = "thirdparty-service", contextId = "thirdparty-service-internal", path = "/thirdparty")
public interface ThirdPartyServiceClient {

    @GetMapping("/internal/third-parties/user-by-document")
    UserByDocumentDto userByDocument(@RequestParam("documentNumber") String documentNumber);

    /** Espejo de thirdparty.InternalController.UserByDocument. */
    record UserByDocumentDto(UUID userId) {}
}
