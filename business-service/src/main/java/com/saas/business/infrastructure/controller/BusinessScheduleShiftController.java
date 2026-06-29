package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BusinessScheduleShiftRequest;
import com.saas.business.application.dto.response.BusinessScheduleShiftResponse;
import com.saas.business.application.mapper.BusinessScheduleShiftMapper;
import com.saas.business.domain.model.BusinessScheduleShift;
import com.saas.business.domain.port.in.IBusinessScheduleShiftUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/business-schedule-shifts")
@RequiredArgsConstructor
public class BusinessScheduleShiftController {
    private final IBusinessScheduleShiftUseCase useCase;
    private final BusinessScheduleShiftMapper mapper;
    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessScheduleShiftResponse>>> byBusinessSchedule(@RequestParam UUID businessScheduleId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBusinessSchedule(businessScheduleId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessScheduleShiftResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<BusinessScheduleShiftResponse>> create(@Valid @RequestBody BusinessScheduleShiftRequest req) {
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(useCase.create(mapper.toDomain(req)))));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessScheduleShiftResponse>> update(@PathVariable UUID id, @Valid @RequestBody BusinessScheduleShiftRequest req) {
        BusinessScheduleShift e = useCase.getById(id); mapper.updateDomain(req, e);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, e))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Turno eliminado"));
    }
}
