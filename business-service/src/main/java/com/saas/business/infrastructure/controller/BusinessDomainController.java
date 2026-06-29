package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BusinessDomainRequest;
import com.saas.business.application.dto.response.BusinessDomainResponse;
import com.saas.business.application.mapper.BusinessDomainMapper;
import com.saas.business.domain.model.BusinessDomain;
import com.saas.business.domain.port.in.IBusinessDomainUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/business-domains")
@RequiredArgsConstructor
public class BusinessDomainController {

    private final IBusinessDomainUseCase useCase;
    private final BusinessDomainMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessDomainResponse>>> list(@RequestParam(required = false) UUID businessId) {
        List<BusinessDomain> domains = businessId != null
                ? useCase.findByBusinessId(businessId)
                : useCase.getAll();
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(domains)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessDomainResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessDomainResponse>> create(@Valid @RequestBody BusinessDomainRequest req) {
        BusinessDomain created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessDomainResponse>> update(@PathVariable UUID id,
                                                                      @Valid @RequestBody BusinessDomainRequest req) {
        BusinessDomain existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Dominio deshabilitado"));
    }
}
