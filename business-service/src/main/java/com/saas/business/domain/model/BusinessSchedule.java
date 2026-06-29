package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/** Plantilla de horario de la empresa. Versionada (ValidFrom/ValidTo). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class BusinessSchedule extends BaseDomain implements ITenantOwned {
    private UUID businessId;
    private UUID scheduleTypeId;   // FK catalogo schedule_type (continuo/discontinuo)
    private String name;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;  // null = vigente
}
