package com.saas.search.domain.document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(
        indexName = "#{@indexNames.roles()}",
        createIndex = false,
        writeTypeHint = WriteTypeHint.FALSE
)public class RoleDocument extends BaseDocument {

    @Field(type = FieldType.Keyword)
    private String code;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Boolean)
    private Boolean enabled;
}