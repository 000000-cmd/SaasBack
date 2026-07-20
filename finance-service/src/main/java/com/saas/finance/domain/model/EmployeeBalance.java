package com.saas.finance.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Saldo por cobrar de un empleado (read model materializado). Se refresca por
 * evento cuando cambia una de sus entradas y se proyecta a Elasticsearch.
 *
 * <p>{@code amountAccrued} y {@code amountPaid} arrancan en 0: aun no existe el
 * modulo de servicios prestados/pagos que los alimente. {@code balance} =
 * amountAccrued - amountPaid.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class EmployeeBalance extends BaseDomain {
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
}
