package com.saas.business.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AgendaExceptionResponse(
        UUID id, UUID businessId, UUID branchId, UUID employeeId,
        Instant startUtc, Instant endUtc, String kind, String kindLabel,
        String reason, Boolean enabled
) {}
