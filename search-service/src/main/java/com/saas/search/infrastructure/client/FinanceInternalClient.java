package com.saas.search.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/** Feign a finance-service (S2S) para el reindex de saldos de empleado. */
@FeignClient(
        name = "finance-service",
        contextId = "finance-internal-search",
        path = "/finance"
)
public interface FinanceInternalClient {

    @GetMapping("/internal/balances/all")
    List<JsonNode> fetchBalances(
            @RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetMapping("/internal/balances/count")
    Map<String, Long> countBalances();

    @GetMapping("/internal/balances/one/{id}")
    JsonNode fetchBalance(@org.springframework.web.bind.annotation.PathVariable("id") String id);
}
