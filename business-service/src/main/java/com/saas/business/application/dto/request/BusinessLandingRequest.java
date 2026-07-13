package com.saas.business.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Documento completo de la landing (el editor manda todos los campos). Todos
 * opcionales: una landing puede publicarse con lo mínimo (branding + servicios
 * ya vienen de sus propias tablas).
 */
public record BusinessLandingRequest(
        @Size(max = 160) String tagline,
        @Size(max = 4000) String about,
        @Size(max = 40) String phone,
        @Size(max = 40) String whatsapp,
        @Email @Size(max = 120) String contactEmail,
        @Size(max = 160) String instagram,
        @Size(max = 160) String facebook,
        @Size(max = 500) String heroImageUrl,
        @Size(max = 4000) String galleryJson,
        @Size(max = 400) String scheduleText,
        Boolean published
) {}
