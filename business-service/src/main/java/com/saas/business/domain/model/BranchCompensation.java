package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Configuracion de pago a nivel SEDE. Excepcion sobre la del negocio; base para el empleado. Versionada. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class BranchCompensation extends BaseDomain {
    private UUID branchId;
    private String compensationType; // SALARY_ONLY|SALARY_PLUS_COMMISSION|SALARY_PLUS_SERVICE_PERCENT|SERVICE_PERCENT_ONLY
    // Valor unico condicionado por compensationType (monto fijo o porcentaje segun el tipo).
    private BigDecimal compensationValue;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
