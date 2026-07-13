package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import lombok.*;

import java.util.UUID;

/**
 * Contenido de la página pública del negocio (la landing a la que se llega por
 * el subdominio/slug). 1:1 con {@link Business}: el dueño la edita con preview
 * en vivo desde "Mi página" y decide cuándo publicarla.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class BusinessLanding extends BaseDomain implements ITenantOwned {
    private UUID businessId;
    private String tagline;
    private String about;
    private String phone;
    private String whatsapp;
    private String contactEmail;
    private String instagram;
    private String facebook;
    private String heroImageUrl;
    /** URLs de la galería serializadas como arreglo JSON. */
    private String galleryJson;
    private String scheduleText;
    /** Solo la landing publicada es visible en el sitio público. */
    private Boolean published;

    @Override
    public UUID getBusinessId() { return businessId; }
}
