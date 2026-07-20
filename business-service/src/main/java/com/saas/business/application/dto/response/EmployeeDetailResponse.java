package com.saas.business.application.dto.response;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Empleado con la persona ya resuelta (nombre + foto) para listados y perfiles
 * del dueño (p.ej. la compensación individual muestra su tarjeta).
 */
public record EmployeeDetailResponse(
        UUID id, UUID thirdPartyId, String personName, String photoUrl,
        UUID branchId, UUID positionId, UUID specialtyId, String employeeCode,
        LocalDate hireDate, LocalDate terminationDate, UUID statusId, Boolean enabled
) {}
