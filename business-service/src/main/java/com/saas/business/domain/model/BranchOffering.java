package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Ajuste por sede de un servicio (deriva de business_offering o propio de la sede). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class BranchOffering extends BaseDomain {
    private UUID branchId;
    private UUID offeringId;  // null = creado solo para la sede
    private String name;             // override (null = hereda)
    private String description;
    private Integer durationMinutes;
    private BigDecimal price;
    private Boolean isEnabled;       // la sede puede inhabilitarlo
    private Boolean isActive;
}
