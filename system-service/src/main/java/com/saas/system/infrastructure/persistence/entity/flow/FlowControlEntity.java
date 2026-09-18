package com.saas.system.infrastructure.persistence.entity.flow;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.saas.system.domain.model.flow.FlowControlAction;

import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "flow_control")
@SQLRestriction("Visible = 1")
public class FlowControlEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "SectionId", length = 36, nullable = false)
    private UUID sectionId;
    @Column(name = "Code", nullable = false, length = 40) private String code;
    @Column(name = "Label", nullable = false, length = 120) private String label;
    @Enumerated(EnumType.STRING) @Column(name = "Action", nullable = false, length = 20)
    private FlowControlAction action = FlowControlAction.CUSTOM;
    @Column(name = "Variant", nullable = false, length = 16) private String variant = "primary";
    @Column(name = "PermissionCode", length = 50) private String permissionCode;
    @Column(name = "Channels", nullable = false, length = 120)
    private String channels = "WEB,PANEL,APK,WHATSAPP";
    @Column(name = "DisplayOrder", nullable = false) private Integer displayOrder = 0;
    @Column(name = "IsSystem", nullable = false) private Boolean isSystem = Boolean.FALSE;
}
