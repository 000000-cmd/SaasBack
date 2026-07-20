package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeRequest(
        @NotNull UUID thirdPartyId,
        @NotNull UUID branchId,
        @NotNull UUID positionId,
        UUID specialtyId,
        @Size(max = 40) String employeeCode,
        @NotNull LocalDate hireDate,
        LocalDate terminationDate,
        UUID statusId
) {}
