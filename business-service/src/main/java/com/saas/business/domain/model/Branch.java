package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Sede de una empresa. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Branch extends BaseDomain implements ITenantOwned {
    private UUID businessId;
    private UUID branchTypeId;     // FK catalogo branch_type
    private String name;
    private String code;
    private UUID municipalityId;   // base de localizacion
    private UUID neighborhoodId;
    private String addressLine;
    /**
     * Donde esta en el mapa. NULL a proposito mientras el dueño no lo marque:
     * el local sale en el directorio pero no en el mapa, que es mejor que un
     * pin puesto en el centro de la ciudad.
     */
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private Boolean isMain;
    private UUID statusId;
}
