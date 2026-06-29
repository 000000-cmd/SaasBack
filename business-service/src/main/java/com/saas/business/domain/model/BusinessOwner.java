package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Relacion empresa-tercero con % de adquisicion (dueno). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class BusinessOwner extends BaseDomain implements ITenantOwned {
    private UUID businessId;
    private UUID thirdPartyId;
    private BigDecimal ownershipPercentage;
    private LocalDate startDate;
    private LocalDate endDate;
}
