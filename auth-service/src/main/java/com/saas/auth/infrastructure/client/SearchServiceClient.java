package com.saas.auth.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign a search-service para resolver la cuenta duena de un documento desde el
 * read model de Elasticsearch. El login por documento es la ruta mas caliente
 * del sistema: leerla de ES evita pegarle a la BD de thirdparty en cada intento.
 *
 * <p>Es una lectura de RESOLUCION, no de autorizacion: si ES no lo tiene (aun no
 * proyectado), el llamador cae a thirdparty-service, que es la fuente de verdad.
 * La contrasena se sigue validando siempre contra auth.</p>
 */
@FeignClient(name = "search-service", contextId = "search-internal-auth", path = "/search")
public interface SearchServiceClient {

    @GetMapping("/internal/third-parties/user-by-document")
    UserByDocumentDto userByDocument(@RequestParam("documentNumber") String documentNumber);

    record UserByDocumentDto(UUID userId) {}
}
