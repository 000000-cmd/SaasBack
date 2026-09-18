package com.saas.system.infrastructure.persistence.entity.flow;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "flow_field")
@SQLRestriction("Visible = 1")
public class FlowFieldEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "SectionId", length = 36, nullable = false)
    private UUID sectionId;
    @Column(name = "Code", nullable = false, length = 60) private String code;
    @Column(name = "Label", nullable = false, length = 120) private String label;
    @Column(name = "Placeholder", length = 120) private String placeholder;
    @Column(name = "HelpText", length = 300) private String helpText;
    @Column(name = "DataType", nullable = false, length = 20) private String dataType = "text";
    @Column(name = "MaskCode", length = 20) private String maskCode;
    @Column(name = "SourceKey", length = 60) private String sourceKey;
    @Column(name = "IsRequired", nullable = false) private Boolean isRequired = Boolean.FALSE;
    @Column(name = "Width", nullable = false, length = 10) private String width = "full";
    @Column(name = "DisplayOrder", nullable = false) private Integer displayOrder = 0;
    @Column(name = "IsSystem", nullable = false) private Boolean isSystem = Boolean.FALSE;
}
