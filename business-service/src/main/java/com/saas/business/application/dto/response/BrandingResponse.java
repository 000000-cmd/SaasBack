package com.saas.business.application.dto.response;

import java.util.UUID;

/** Branding público de una empresa, resuelto por slug (para tematizar el login). */
public record BrandingResponse(
        UUID businessId,
        String name,
        String logoUrl,
        String primaryColor,
        String secondaryColor,
        String slug
) {}
