package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;
import java.util.UUID;

/** Cliente global (reutilizable por cualquier empresa). Deriva de un tercero. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Client extends BaseDomain {
    private UUID thirdPartyId;          // unico
    private UUID registrationStatusId;  // FK catalogo registration_status
    private String acquisitionSource;
    private String notes;
}
