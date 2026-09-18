package com.saas.system.infrastructure.persistence.entity.flow;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.saas.system.domain.model.flow.FlowSectionKind;

import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "flow_section")
@SQLRestriction("Visible = 1")
public class FlowSectionEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "FlowId", length = 36, nullable = false)
    private UUID flowId;
    @Column(name = "Code", nullable = false, length = 40) private String code;
    @Column(name = "Name", nullable = false, length = 120) private String name;
    @Column(name = "Description", length = 500) private String description;
    @Column(name = "DisplayOrder", nullable = false) private Integer displayOrder = 0;
    @Enumerated(EnumType.STRING) @Column(name = "Kind", nullable = false, length = 16)
    private FlowSectionKind kind = FlowSectionKind.FORM;
    @Column(name = "Channels", nullable = false, length = 120)
    private String channels = "WEB,PANEL,APK,WHATSAPP";
    @Column(name = "IsSystem", nullable = false) private Boolean isSystem = Boolean.FALSE;
}
