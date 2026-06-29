package com.saas.thirdparty.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.util.UUID;

/** Direccion de un tercero (1:N). MunicipalityId es la base de localizacion. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class ThirdPartyAddress extends BaseDomain {
    private UUID thirdPartyId;
    private UUID addressTypeId;     // FK catalogo address_type (system)
    private UUID municipalityId;    // base: resuelve department + country
    private UUID neighborhoodId;    // detalle fino opcional
    private String line;
    private String reference;
    private Boolean isPrimary;
}
