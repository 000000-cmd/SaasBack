package com.saas.finance.application.dto.event;

import com.saas.finance.domain.model.EmployeeBalance;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload publicado al outbox para indexar el saldo del empleado en
 * Elasticsearch (indice employee_balances). El APK lo lee desde ES.
 */
@Data
@Builder
public class EmployeeBalanceEventPayload {
    private UUID id;
    private UUID businessId;
    private UUID branchId;
    private UUID employeeId;
    private UUID thirdPartyId;
    private UUID userId;
    private BigDecimal amountAccrued;
    private BigDecimal amountPaid;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime lastCalculatedAt;

    public static EmployeeBalanceEventPayload from(EmployeeBalance b) {
        return EmployeeBalanceEventPayload.builder()
                .id(b.getId())
                .businessId(b.getBusinessId())
                .branchId(b.getBranchId())
                .employeeId(b.getEmployeeId())
                .thirdPartyId(b.getThirdPartyId())
                .userId(b.getUserId())
                .amountAccrued(b.getAmountAccrued())
                .amountPaid(b.getAmountPaid())
                .balance(b.getBalance())
                .currency(b.getCurrency())
                .lastCalculatedAt(b.getLastCalculatedAt())
                .build();
    }
}
