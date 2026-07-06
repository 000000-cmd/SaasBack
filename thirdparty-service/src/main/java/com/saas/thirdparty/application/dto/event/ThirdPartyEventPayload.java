package com.saas.thirdparty.application.dto.event;

import com.saas.thirdparty.domain.model.ThirdParty;
import com.saas.thirdparty.domain.model.ThirdPartyAddress;
import com.saas.thirdparty.domain.model.ThirdPartyContact;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Payload publicado al outbox para indexacion en Elasticsearch (search-service).
 *
 * <p>Lleva los campos planos del LISTADO/BUSQUEDA (documento, nombre, estado, Ids
 * de filtro) MAS el detalle anidado (contactos + direcciones). Asi el documento
 * indexado en ES contiene lo mismo que {@code /full} de la BD y el comparador de
 * reindex confronta "ES full" vs "BD full" campo a campo (sin endpoints extra).</p>
 *
 * <p>Los sublistados NO se indexan (mapping {@code enabled:false}): viven en el
 * {@code _source} para leerse/compararse, sin engordar el indice de busqueda.</p>
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

    private List<Contact> contacts;
    private List<Address> addresses;

    @Data
    @Builder
    public static class Contact {
        private UUID id;
        private UUID contactTypeId;
        private String value;
        private Boolean isPrimary;
        private Boolean isVerified;
        private String notes;
        private Boolean enabled;
    }

    @Data
    @Builder
    public static class Address {
        private UUID id;
        private UUID addressTypeId;
        private UUID municipalityId;
        private UUID neighborhoodId;
        private String line;
        private String reference;
        private Boolean isPrimary;
        private Boolean enabled;
    }

    /** Solo base (para el evento de borrado, que no necesita hijos). */
    public static ThirdPartyEventPayload from(ThirdParty t) {
        return from(t, List.of(), List.of());
    }

    /** Documento COMPLETO: base + contactos + direcciones. */
    public static ThirdPartyEventPayload from(ThirdParty t,
                                              List<ThirdPartyContact> contacts,
                                              List<ThirdPartyAddress> addresses) {
        return ThirdPartyEventPayload.builder()
                .id(t.getId())
                .documentTypeId(t.getDocumentTypeId())
                .documentNumber(t.getDocumentNumber())
                .firstName(t.getFirstName())
                .firstLastName(t.getFirstLastName())
                .fullName(buildFullName(t))
                .genderId(t.getGenderId())
                .enabled(t.getEnabled())
                .contacts(contacts.stream()
                        .map(c -> Contact.builder()
                                .id(c.getId())
                                .contactTypeId(c.getContactTypeId())
                                .value(c.getValue())
                                .isPrimary(c.getIsPrimary())
                                .isVerified(c.getIsVerified())
                                .notes(c.getNotes())
                                .enabled(c.getEnabled())
                                .build())
                        .toList())
                .addresses(addresses.stream()
                        .map(a -> Address.builder()
                                .id(a.getId())
                                .addressTypeId(a.getAddressTypeId())
                                .municipalityId(a.getMunicipalityId())
                                .neighborhoodId(a.getNeighborhoodId())
                                .line(a.getLine())
                                .reference(a.getReference())
                                .isPrimary(a.getIsPrimary())
                                .enabled(a.getEnabled())
                                .build())
                        .toList())
                .build();
    }

    private static String buildFullName(ThirdParty t) {
        return String.join(" ",
                Stream.of(t.getFirstName(), t.getSecondName(), t.getFirstLastName(), t.getSecondLastName())
                        .filter(s -> s != null && !s.isBlank())
                        .toList());
    }
}
