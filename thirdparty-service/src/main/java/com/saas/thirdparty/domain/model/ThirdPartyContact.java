package com.saas.thirdparty.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Contacto de un tercero (1:N): celular, email, redes, etc. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class ThirdPartyContact extends BaseDomain {
    private UUID thirdPartyId;
    private UUID contactTypeId;   // FK catalogo contact_type (system)
    private String value;
    private Boolean isPrimary;
    private Boolean isVerified;
    private LocalDateTime verifiedAt;
    private String notes;
}
