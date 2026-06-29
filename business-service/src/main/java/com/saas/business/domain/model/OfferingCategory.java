package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import lombok.*;
import java.util.UUID;

/** Categoria de ofertas de una empresa (corte, color, manicure...). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class OfferingCategory extends BaseDomain implements ITenantOwned {
    private UUID businessId;
    private String name;
    private Integer displayOrder;
}
