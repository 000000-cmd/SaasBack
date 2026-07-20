package com.saas.search.domain.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.List;
import java.util.UUID;

/**
 * Read model de Terceros (persona natural).
 *
 * <p>Los campos planos (documento, nombre, estado, Ids de filtro) sirven al
 * LISTADO/BUSQUEDA. Ademas guarda el detalle anidado (contactos + direcciones)
 * en el {@code _source} —no indexado (mapping {@code enabled:false})— para que
 * el documento sea equivalente a {@code /full} de la BD y el comparador de
 * reindex confronte ES-full vs BD-full.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(
        indexName = "#{@indexNames.thirdParties()}",
        createIndex = false,
        writeTypeHint = WriteTypeHint.FALSE
)
public class ThirdPartyDocument extends BaseDocument {

    /** Cuenta vinculada: permite resolver persona<->usuario sin pegarle a la BD. */
    @Field(type = FieldType.Keyword)
    private UUID userId;

    @Field(type = FieldType.Keyword)
    private String documentNumber;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) }
    )
    private String fullName;

    @Field(type = FieldType.Text)
    private String firstName;

    @Field(type = FieldType.Text)
    private String firstLastName;

    /** Foto de perfil (la sube el empleado desde el APK). Para tarjetas de persona. */
    @Field(type = FieldType.Keyword, index = false)
    private String photoUrl;

    /** Ids para filtrar (sin desnormalizar: el nombre se resuelve en el front). */
    @Field(type = FieldType.Keyword)
    private UUID documentTypeId;

    @Field(type = FieldType.Keyword)
    private UUID genderId;

    @Field(type = FieldType.Boolean)
    private Boolean enabled;

    /** Detalle anidado: en _source, no indexado (ver mapping). */
    @Field(type = FieldType.Object, enabled = false)
    private List<Contact> contacts;

    @Field(type = FieldType.Object, enabled = false)
    private List<Address> addresses;

    @Data
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
}
