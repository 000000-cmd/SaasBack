package com.saas.business.application.dto.response;

import java.util.UUID;

public record EmployeeProvisionResponse(
        UUID employeeId,
        UUID thirdPartyId,
        UUID userId,
        String username
) {}
