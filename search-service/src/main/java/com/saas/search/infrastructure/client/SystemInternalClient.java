package com.saas.search.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign client a system-service via Eureka.
 *
 * <p>El atributo {@code path = "/system"} corresponde al
 * {@code server.servlet.context-path} de system-service.
 */
@FeignClient(
        name = "system-service",
        contextId = "system-internal-search",
        path = "/system"
)
public interface SystemInternalClient {

    @GetMapping("/internal/roles/all")
    List<JsonNode> fetchRoles(
            @RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetMapping("/internal/roles/count")
    Map<String, Long> countRoles();
}
