package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Servicio que ofrece la empresa (plantilla). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Offering extends BaseDomain implements ITenantOwned {
    private UUID businessId;
    private UUID categoryId;
    private UUID specialtyId;
    private String name;
    private String description;
    private Integer durationMinutes;
    private BigDecimal price;
    private Boolean isActive;
}
