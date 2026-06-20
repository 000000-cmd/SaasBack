package com.saas.audit.infrastructure.controller;

import com.saas.audit.application.dto.AuditLogResponse;
import com.saas.audit.application.service.AuditService;
import com.saas.common.audit.AuditAction;
import com.saas.common.dto.ApiResponse;
import com.saas.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * API de consulta de auditoria. El gateway expone esto bajo {@code /audit}.
 * Todos los filtros son opcionales y combinables.
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> search(
            @RequestParam(required = false) UUID businessId,
            @RequestParam(required = false) String aggregateType,
            @RequestParam(required = false) UUID aggregateId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<AuditLogResponse> result =
                auditService.search(businessId, aggregateType, aggregateId, actorId, action, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
