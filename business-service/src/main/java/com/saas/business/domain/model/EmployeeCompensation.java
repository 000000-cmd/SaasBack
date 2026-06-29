package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Configuracion de pago de un empleado. Versionada. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class EmployeeCompensation extends BaseDomain {
    private UUID employeeId;
    private String compensationType; // SALARY_ONLY|SALARY_PLUS_COMMISSION|SALARY_PLUS_SERVICE_PERCENT|SERVICE_PERCENT_ONLY
    private BigDecimal baseSalary;
    private BigDecimal servicePercentage;
    private BigDecimal fixedCommission;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
