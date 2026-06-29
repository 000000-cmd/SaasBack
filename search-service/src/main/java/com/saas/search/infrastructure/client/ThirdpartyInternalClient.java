package com.saas.search.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/** Feign a thirdparty-service (S2S) para el reindex de terceros. */
@FeignClient(
        name = "thirdparty-service",
        contextId = "thirdparty-internal-search",
        path = "/thirdparty"
)
public interface ThirdpartyInternalClient {

    @GetMapping("/internal/third-parties/all")
    List<JsonNode> fetchThirdParties(
            @RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetMapping("/internal/third-parties/count")
    Map<String, Long> countThirdParties();
}
