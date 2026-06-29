package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/** Horario efectivo de una sede (ajuste). Versionado. Puede derivar de una plantilla de empresa. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class BranchSchedule extends BaseDomain {
    private UUID branchId;
    private UUID businessScheduleId;  // null = propio de la sede
    private UUID scheduleTypeId;
    private String name;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
