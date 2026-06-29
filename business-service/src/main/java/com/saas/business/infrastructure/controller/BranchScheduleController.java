package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BranchScheduleRequest;
import com.saas.business.application.dto.response.BranchScheduleResponse;
import com.saas.business.application.mapper.BranchScheduleMapper;
import com.saas.business.domain.model.BranchSchedule;
import com.saas.business.domain.port.in.IBranchScheduleUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/branch-schedules")
@RequiredArgsConstructor
public class BranchScheduleController {
    private final IBranchScheduleUseCase useCase;
    private final BranchScheduleMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchScheduleResponse>>> history(@RequestParam UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBranch(branchId))));
    }
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<BranchScheduleResponse>>> current(@RequestParam UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findCurrentByBranch(branchId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchScheduleResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<BranchScheduleResponse>> create(@Valid @RequestBody BranchScheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(useCase.create(mapper.toDomain(req)))));
    }
    @PutMapping("/{id}/supersede")
    public ResponseEntity<ApiResponse<BranchScheduleResponse>> supersede(@PathVariable UUID id, @Valid @RequestBody BranchScheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.supersede(id, mapper.toDomain(req)))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Horario de sede deshabilitado"));
    }
}
