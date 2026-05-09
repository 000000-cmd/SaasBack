package com.saas.search.domain.document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.document.SearchDocument;

import java.util.List;

/**
 * Read model de usuarios para busqueda. Construido por {@code UserEventHandler}
 * a partir de eventos {@code user.*} consumidos de Kafka.
 *
 * El indice se llama por su alias ({@code users}); por debajo apunta a
 * {@code users_v1} (creado por {@code IndexBootstrap}).
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Document(
        indexName = "#{@indexNames.users()}",
        createIndex = false,
        writeTypeHint = WriteTypeHint.FALSE
)public class UserDocument extends BaseDocument {

    @Field(type = FieldType.Keyword)
    private String username;

    @Field(type = FieldType.Keyword)
    private String email;

    /**
     * Texto buscable + sub-campo keyword para ordenamiento exacto.
     *
     * {@code fullName} (FieldType.Text) → "juan perez" se tokeniza en
     * ["juan","perez"] → busca "Juan" o "perez" lo encuentra.
     *
     * {@code fullName.keyword} (FieldType.Keyword) → texto completo sin
     * analizar. Para "ORDER BY fullName" exacto.
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String fullName;

    @Field(type = FieldType.Boolean)
    private Boolean enabled;

    @Field(type = FieldType.Keyword)
    private List<String> roleCodes;
}
