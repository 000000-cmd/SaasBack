package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import lombok.*;

import java.util.UUID;

/** Empresa (el local: barberia/salon). Incluye la identidad juridica. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Business extends BaseDomain implements ITenantOwned {
    private UUID businessTypeId;   // FK catalogo business_type
    private String name;
    private String legalName;
    private String tradeName;
    private UUID documentTypeId;   // FK catalogo document_type (NIT)
    private String documentNumber;
    private String logoUrl;
    private UUID statusId;                  // FK catalogo status
    private String primaryColor;
    private String secondaryColor;

    /**
     * Zona horaria del negocio. La agenda guarda SIEMPRE UTC y usa esto para
     * presentarla; tambien la necesitan los cortes de nomina y los informes.
     */
    @Builder.Default
    private String timeZone = "America/Bogota";

    /** La empresa ES el tenant: su propio Id es el businessId. */
    @Override
    public UUID getBusinessId() { return getId(); }
}
