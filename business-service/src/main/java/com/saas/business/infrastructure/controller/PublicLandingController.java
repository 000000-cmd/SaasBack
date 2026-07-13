package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.response.PublicLandingResponse;
import com.saas.business.domain.model.Business;
import com.saas.business.domain.model.BusinessDomain;
import com.saas.business.domain.model.BusinessLanding;
import com.saas.business.domain.port.in.IBranchUseCase;
import com.saas.business.domain.port.in.IBusinessDomainUseCase;
import com.saas.business.domain.port.in.IBusinessLandingUseCase;
import com.saas.business.domain.port.in.IBusinessUseCase;
import com.saas.business.domain.port.in.IOfferingUseCase;
import com.saas.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Superficie PÚBLICA de la landing del negocio (sin JWT; {@code /business/public}
 * está en los OPEN_PREFIXES del gateway):
 *  - {@code GET /public/landing?slug=}: agrega en UN request todo lo que la
 *    página necesita (branding + contenido + sedes + servicios activos).
 *    404 si el slug no existe o la landing no está publicada.
 *  - {@code GET /public/landing-assets/{filename}}: sirve las imágenes subidas
 *    desde el editor.
 */
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicLandingController {

    private final IBusinessDomainUseCase domainUseCase;
    private final IBusinessUseCase businessUseCase;
    private final IBusinessLandingUseCase landingUseCase;
    private final IBranchUseCase branchUseCase;
    private final IOfferingUseCase offeringUseCase;

    @Value("${business.landing.storage-dir:./storage/landing}")
    private String storageDir;

    @GetMapping("/landing")
    public ResponseEntity<ApiResponse<PublicLandingResponse>> landing(@RequestParam String slug) {
        Optional<BusinessDomain> domain = domainUseCase.findBySlug(slug);
        if (domain.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("Slug no encontrado", 404));
        }
        UUID businessId = domain.get().getBusinessId();
        Optional<BusinessLanding> landing = landingUseCase.findByBusiness(businessId);
        if (landing.isEmpty() || !Boolean.TRUE.equals(landing.get().getPublished())) {
            return ResponseEntity.ok(ApiResponse.error("Página no publicada", 404));
        }

        Business b = businessUseCase.getById(businessId);
        BusinessLanding l = landing.get();

        List<PublicLandingResponse.BranchInfo> branches = branchUseCase.findByBusiness(businessId).stream()
                .filter(br -> !Boolean.FALSE.equals(br.getEnabled()))
                .map(br -> new PublicLandingResponse.BranchInfo(br.getName(), br.getAddressLine(), br.getPhone()))
                .toList();

        List<PublicLandingResponse.OfferingInfo> offerings = offeringUseCase.findByBusiness(businessId).stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsActive()))
                .map(o -> new PublicLandingResponse.OfferingInfo(
                        o.getName(), o.getDescription(), o.getPrice(), o.getDurationMinutes()))
                .toList();

        PublicLandingResponse body = new PublicLandingResponse(
                new PublicLandingResponse.BusinessInfo(b.getId(), b.getName(), b.getLogoUrl(),
                        b.getPrimaryColor(), b.getSecondaryColor(), slug),
                new PublicLandingResponse.LandingInfo(l.getTagline(), l.getAbout(), l.getPhone(), l.getWhatsapp(),
                        l.getContactEmail(), l.getInstagram(), l.getFacebook(),
                        l.getHeroImageUrl(), l.getGalleryJson(), l.getScheduleText()),
                branches, offerings);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    @GetMapping("/landing-assets/{filename}")
    public ResponseEntity<Resource> asset(@PathVariable String filename) {
        // Anti path-traversal: solo nombres planos generados por el upload.
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path file = Path.of(storageDir).toAbsolutePath().normalize().resolve(filename);
            if (!Files.exists(file)) return ResponseEntity.notFound().build();
            String contentType = Files.probeContentType(file);
            Resource resource = new UrlResource(file.toUri());
            return ResponseEntity.ok()
                    .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (java.io.IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
