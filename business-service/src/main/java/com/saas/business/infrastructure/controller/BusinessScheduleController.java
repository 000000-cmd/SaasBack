package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BusinessScheduleRequest;
import com.saas.business.application.dto.response.BusinessScheduleResponse;
import com.saas.business.application.mapper.BusinessScheduleMapper;
import com.saas.business.domain.model.BusinessSchedule;
import com.saas.business.domain.port.in.IBusinessScheduleUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/business-schedules")
@RequiredArgsConstructor
public class BusinessScheduleController {
    private final IBusinessScheduleUseCase useCase;
    private final BusinessScheduleMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessScheduleResponse>>> history(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBusiness(businessId))));
    }
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<BusinessScheduleResponse>>> current(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findCurrentByBusiness(businessId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessScheduleResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<BusinessScheduleResponse>> create(@Valid @RequestBody BusinessScheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(useCase.create(mapper.toDomain(req)))));
    }
    /** Cambio versionado: cierra la version vigente y crea una nueva. */
    @PutMapping("/{id}/supersede")
    public ResponseEntity<ApiResponse<BusinessScheduleResponse>> supersede(@PathVariable UUID id, @Valid @RequestBody BusinessScheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.supersede(id, mapper.toDomain(req)))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Horario deshabilitado"));
    }
}
