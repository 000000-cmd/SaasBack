package com.saas.system.infrastructure.persistence.entity.flow;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Getter @Setter
@Entity @Table(name = "flow")
@SQLRestriction("Visible = 1")
public class FlowEntity extends BaseEntity {
    @Column(name = "Code", nullable = false, length = 40) private String code;
    @Column(name = "Name", nullable = false, length = 120) private String name;
    @Column(name = "Description", length = 500) private String description;
    @Column(name = "IsSystem", nullable = false) private Boolean isSystem = Boolean.FALSE;
}
