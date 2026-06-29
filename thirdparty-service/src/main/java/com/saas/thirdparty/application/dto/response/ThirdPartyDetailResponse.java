package com.saas.thirdparty.application.dto.response;

import java.util.List;

/**
 * Vista completa y anidada de un tercero: sus datos base + contactos + direcciones,
 * en un solo objeto. La consume la acción "visualizar" del front.
 */
public record ThirdPartyDetailResponse(
        ThirdPartyResponse thirdParty,
        List<ThirdPartyContactResponse> contacts,
        List<ThirdPartyAddressResponse> addresses
) {}
