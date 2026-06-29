package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

/** Empleado: hereda info base del tercero (por UUID) y pertenece a una sede. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Employee extends BaseDomain {
    private UUID thirdPartyId;
    private UUID branchId;
    private UUID positionId;     // FK catalogo employee_position
    private String employeeCode;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private UUID statusId;
}
