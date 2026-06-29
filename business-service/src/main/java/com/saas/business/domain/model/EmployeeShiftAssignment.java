package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/** Asignacion de un turno de sede a un empleado. Versionada (trazabilidad). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class EmployeeShiftAssignment extends BaseDomain {
    private UUID employeeId;
    private UUID branchScheduleShiftId;
    private Boolean isFullShift;
    private LocalTime customStartTime;  // si no es completo
    private LocalTime customEndTime;
    private UUID statusId;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
