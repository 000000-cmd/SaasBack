package com.saas.business.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeResponse(
        UUID id, UUID thirdPartyId, UUID branchId, UUID positionId, String employeeCode,
        LocalDate hireDate, LocalDate terminationDate, UUID statusId,
        Boolean enabled, Boolean visible, LocalDateTime createdDate, LocalDateTime auditDate
) {}
