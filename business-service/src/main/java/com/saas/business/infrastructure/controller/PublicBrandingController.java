package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.response.BrandingResponse;
import com.saas.business.domain.model.Business;
import com.saas.business.domain.model.BusinessDomain;
import com.saas.business.domain.port.in.IBusinessDomainUseCase;
import com.saas.business.domain.port.in.IBusinessUseCase;
import com.saas.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints PÚBLICOS (sin JWT). Resuelven el branding de una empresa por su
 * slug para que el front pueda tematizar el login del dueño antes de autenticar.
 */
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicBrandingController {

    private final IBusinessDomainUseCase domainUseCase;
    private final IBusinessUseCase businessUseCase;

    @GetMapping("/branding")
    public ResponseEntity<ApiResponse<BrandingResponse>> branding(@RequestParam String slug) {
        return domainUseCase.findBySlug(slug)
                .map(BusinessDomain::getBusinessId)
                .map(businessUseCase::getById)
                .map(b -> brandingOf(b, slug))
                .map(ApiResponse::success)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("Slug no encontrado", 404)));
    }

    private BrandingResponse brandingOf(Business b, String slug) {
        return new BrandingResponse(
                b.getId(), b.getName(), b.getLogoUrl(),
                b.getPrimaryColor(), b.getSecondaryColor(), slug);
    }
}
