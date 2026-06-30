package com.saas.business.application.dto.response;

import java.time.LocalDate;
import java.util.UUID;

/** Empleado con el nombre de la persona ya resuelto (para listados del dueño). */
public record EmployeeDetailResponse(
        UUID id, UUID thirdPartyId, String personName,
        UUID branchId, UUID positionId, String employeeCode,
        LocalDate hireDate, LocalDate terminationDate, UUID statusId, Boolean enabled
) {}
