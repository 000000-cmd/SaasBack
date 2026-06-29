package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BranchScheduleShiftRequest;
import com.saas.business.application.dto.response.BranchScheduleShiftResponse;
import com.saas.business.application.mapper.BranchScheduleShiftMapper;
import com.saas.business.domain.model.BranchScheduleShift;
import com.saas.business.domain.port.in.IBranchScheduleShiftUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/branch-schedule-shifts")
@RequiredArgsConstructor
public class BranchScheduleShiftController {
    private final IBranchScheduleShiftUseCase useCase;
    private final BranchScheduleShiftMapper mapper;
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchScheduleShiftResponse>>> byBranchSchedule(@RequestParam UUID branchScheduleId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBranchSchedule(branchScheduleId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchScheduleShiftResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<BranchScheduleShiftResponse>> create(@Valid @RequestBody BranchScheduleShiftRequest req) {
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(useCase.create(mapper.toDomain(req)))));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchScheduleShiftResponse>> update(@PathVariable UUID id, @Valid @RequestBody BranchScheduleShiftRequest req) {
        BranchScheduleShift e = useCase.getById(id); mapper.updateDomain(req, e);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, e))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Turno eliminado"));
    }
}
