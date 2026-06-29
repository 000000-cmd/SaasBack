package com.saas.search.domain.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.UUID;

/**
 * Read model de Terceros (persona natural) para LISTADO/BUSQUEDA.
 *
 * <p>Solo lleva lo que la lista muestra/busca: documento, nombre y estado, mas
 * los Ids que sirven de filtro. El detalle completo (contactos, direcciones,
 * nombres de catalogos) se obtiene de la BD via {@code /full}; ES no los duplica.</p>
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

    /** Ids para filtrar (sin desnormalizar: el nombre se resuelve en el front). */
    @Field(type = FieldType.Keyword)
    private UUID documentTypeId;

    @Field(type = FieldType.Keyword)
    private UUID genderId;

    @Field(type = FieldType.Boolean)
    private Boolean enabled;
}
