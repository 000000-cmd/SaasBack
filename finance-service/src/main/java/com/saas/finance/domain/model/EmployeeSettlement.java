package com.saas.finance.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Liquidacion confirmada a un empleado: mueve dinero del negocio a su saldo
 * (suma a amountPaid y baja el por cobrar). Es irreversible, por eso cada
 * confirmacion queda registrada aqui como auditoria de tesoreria.
 *
 * <p>{@code balanceBefore} congela el saldo por cobrar previo para poder
 * auditar sin recalcular historia.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class EmployeeSettlement extends BaseDomain {
    private UUID businessId;
    private UUID branchId;
    private UUID employeeId;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private String currency;
    private LocalDateTime settledAt;
    private String note;
}
