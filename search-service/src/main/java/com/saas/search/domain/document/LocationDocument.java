package com.saas.search.domain.document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

/**
 * Read model desnormalizado para busqueda de localizacion.
 *
 * <p>Cada doc representa UN nivel (PAIS, DEPARTAMENTO, MUNICIPIO, BARRIO/VEREDA)
 * pero lleva embebida toda su cadena hacia arriba. Asi un solo indice
 * {@code locations} resuelve cualquier busqueda jerarquica con 1 query.</p>
 *
 * <p>Campos de nombre usan {@code search_as_you_type} para soportar busqueda
 * incremental (>= 3 letras nativas). Codigos son keyword para filtros exactos.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(
        indexName = "#{@indexNames.locations()}",
        createIndex = false,
        writeTypeHint = WriteTypeHint.FALSE
)
public class LocationDocument extends BaseDocument {

    /** PAIS | DEPARTAMENTO | MUNICIPIO | BARRIO | VEREDA | CORREGIMIENTO | OTRO. */
    @Field(type = FieldType.Keyword)
    private String level;

    // ---------- Pais ----------
    @Field(type = FieldType.Keyword)
    private String countryId;

    @Field(type = FieldType.Keyword)
    private String countryCode;

    @MultiField(
            mainField = @Field(type = FieldType.Search_As_You_Type),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String countryName;

    // ---------- Departamento ----------
    @Field(type = FieldType.Keyword)
    private String departmentId;

    @Field(type = FieldType.Keyword)
    private String departmentCode;

    @MultiField(
            mainField = @Field(type = FieldType.Search_As_You_Type),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String departmentName;

    // ---------- Municipio ----------
    @Field(type = FieldType.Keyword)
    private String municipalityId;

    @Field(type = FieldType.Keyword)
    private String municipalityCode;

    @MultiField(
            mainField = @Field(type = FieldType.Search_As_You_Type),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String municipalityName;

    // ---------- Barrio / Vereda ----------
    @Field(type = FieldType.Keyword)
    private String neighborhoodId;

    @Field(type = FieldType.Keyword)
    private String neighborhoodCode;

    @MultiField(
            mainField = @Field(type = FieldType.Search_As_You_Type),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String neighborhoodName;

    @Field(type = FieldType.Keyword)
    private String neighborhoodType;

    /** Texto agregado para busqueda libre cross-field. */
    @Field(type = FieldType.Text, analyzer = "spanish_text")
    private String searchText;

    /** Cadena legible separada por coma: "Colombia, Antioquia, Medellin, El Poblado". */
    @Field(type = FieldType.Text, analyzer = "spanish_text")
    private String fullPath;

    @Field(type = FieldType.Boolean)
    private Boolean enabled;
}
