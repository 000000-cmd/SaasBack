package com.saas.business.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

/**
 * Feign a finance-service para inicializar el saldo del empleado al aprovisionarlo.
 * Solo endpoints {@code /internal/**} (S2S). {@code path} = context-path de finance.
 */
@FeignClient(name = "finance-service", contextId = "finance-internal-business", path = "/finance")
public interface FinanceClient {

    @PostMapping("/internal/balances/ensure")
    void ensureBalance(@RequestBody EnsureBalanceRequest request);

    record EnsureBalanceRequest(UUID employeeId, UUID businessId, UUID branchId, UUID thirdPartyId, UUID userId) {}
}
