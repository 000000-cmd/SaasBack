package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;
import java.time.LocalTime;
import java.util.UUID;

/** Turno (Turno de horario de sede). Hijo de un horario; el horario es el versionado. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class BranchScheduleShift extends BaseDomain {
    private UUID branchScheduleId;
    private UUID shiftTypeId;   // FK catalogo shift_type (manana/tarde/noche)
    private UUID dayOfWeekId;   // FK catalogo day_of_week
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer displayOrder;
}
