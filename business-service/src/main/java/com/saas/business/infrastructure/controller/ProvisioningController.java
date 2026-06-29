package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.ProvisionRequest;
import com.saas.business.application.dto.response.ProvisionResponse;
import com.saas.business.application.service.ProvisioningService;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aprovisionamiento del negocio (post-login del dueño). Ruta efectiva:
 * {@code POST /business/provision} (requiere JWT; no está en la whitelist pública).
 */
@RestController
@RequestMapping("/provision")
@RequiredArgsConstructor
public class ProvisioningController {

    private final ProvisioningService provisioningService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProvisionResponse>> provision(@Valid @RequestBody ProvisionRequest req) {
        return ResponseEntity.ok(ApiResponse.created(provisioningService.provision(req)));
    }
}
