package com.saas.business.application.dto.response;

import java.util.UUID;

public record BusinessLandingResponse(
        UUID id,
        UUID businessId,
        String tagline,
        String about,
        String phone,
        String whatsapp,
        String contactEmail,
        String instagram,
        String facebook,
        String heroImageUrl,
        String galleryJson,
        String scheduleText,
        Boolean published
) {}
