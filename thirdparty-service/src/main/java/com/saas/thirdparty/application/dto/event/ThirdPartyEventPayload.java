package com.saas.thirdparty.application.dto.event;

import com.saas.thirdparty.domain.model.ThirdParty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;
import java.util.stream.Stream;

/**
 * Payload plano publicado al outbox para indexacion en Elasticsearch
 * (search-service). Solo lleva lo que el read model de LISTADO/BUSQUEDA necesita:
 * documento, nombre y estado, mas los Ids de filtro. El detalle (contactos,
 * direcciones, nombres de catalogos) NO se indexa: el front lo pide a la BD.
 */
@Data
@Builder
public class ThirdPartyEventPayload {

    private UUID id;
    private UUID documentTypeId;
    private String documentNumber;
    private String firstName;
    private String firstLastName;
    private String fullName;
    private UUID genderId;
    private Boolean enabled;

    public static ThirdPartyEventPayload from(ThirdParty t) {
        return ThirdPartyEventPayload.builder()
                .id(t.getId())
                .documentTypeId(t.getDocumentTypeId())
                .documentNumber(t.getDocumentNumber())
                .firstName(t.getFirstName())
                .firstLastName(t.getFirstLastName())
                .fullName(buildFullName(t))
                .genderId(t.getGenderId())
                .enabled(t.getEnabled())
                .build();
    }

    private static String buildFullName(ThirdParty t) {
        return String.join(" ",
                Stream.of(t.getFirstName(), t.getSecondName(), t.getFirstLastName(), t.getSecondLastName())
                        .filter(s -> s != null && !s.isBlank())
                        .toList());
    }
}
