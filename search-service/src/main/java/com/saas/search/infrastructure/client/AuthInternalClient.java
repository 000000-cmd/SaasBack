package com.saas.search.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign client a auth-service via Eureka.
 * Solo expone los endpoints internos (S2S) usados para reindex.
 *
 * <p>El atributo {@code path = "/auth"} corresponde al
 * {@code server.servlet.context-path} de auth-service. Asi cada {@code @GetMapping}
 * solo declara el path relativo (sin prefijo). Si algun dia se cambia el context
 * path, solo se modifica este atributo.
 *
 * <p>Usamos {@link JsonNode} en lugar del DTO concreto porque search-service
 * no debe conocer las clases de auth.
 */
@FeignClient(
        name = "auth-service",
        contextId = "auth-internal-search",
        path = "/auth"
)
public interface AuthInternalClient {

    @GetMapping("/internal/users/all")
    List<JsonNode> fetchUsers(
            @RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetMapping("/internal/users/count")
    Map<String, Long> countUsers();
}
