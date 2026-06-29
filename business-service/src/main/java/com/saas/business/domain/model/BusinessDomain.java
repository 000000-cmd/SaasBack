package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dominio/slug de una empresa para tematizar y enrutar el acceso del dueño.
 *
 * <p>Se separa de {@link Business} porque el slug y los dominios tienen su propio
 * ciclo de vida (verificacion, dominio propio, primario) que no debe mezclarse con
 * la identidad de la empresa. Una empresa puede tener varios; uno es el primario.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class BusinessDomain extends BaseDomain implements ITenantOwned {
    private UUID businessId;
    /** Etiqueta de subdominio (p.ej. "mi-barberia"). Unica en el sistema. */
    private String slug;
    /** Dominio propio opcional (p.ej. "citas.mibarberia.com"). */
    private String customDomain;
    private Boolean isPrimary;
    private Boolean isVerified;
    private LocalDateTime verifiedDate;
    private UUID statusId;

    @Override
    public UUID getBusinessId() { return businessId; }
}
