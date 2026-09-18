package com.saas.finance.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * El nombre del negocio, para firmar los correos de dinero. Un extracto que
 * llega sin decir de quien es no lo abre nadie.
 */
@FeignClient(name = "business-service", contextId = "business-for-finance", path = "/business")
public interface BusinessInternalClient {

    record BusinessName(UUID id, String name) {}

    @GetMapping("/internal/businesses/{id}/name")
    BusinessName name(@PathVariable("id") UUID id);
}
