package com.saas.business.application.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Todo lo que la página pública del negocio necesita en UN solo request:
 * identidad/branding + contenido editorial + sedes + servicios activos.
 * Servido por {@code GET /public/landing?slug=} (sin JWT).
 */
public record PublicLandingResponse(
        BusinessInfo business,
        LandingInfo landing,
        List<BranchInfo> branches,
        List<OfferingInfo> offerings
) {
    public record BusinessInfo(UUID id, String name, String logoUrl,
                               String primaryColor, String secondaryColor, String slug) {}

    public record LandingInfo(String tagline, String about, String phone, String whatsapp,
                              String contactEmail, String instagram, String facebook,
                              String heroImageUrl, String galleryJson, String scheduleText) {}

    public record BranchInfo(String name, String addressLine, String phone) {}

    public record OfferingInfo(String name, String description, BigDecimal price, Integer durationMinutes) {}
}
