package com.saas.system.infrastructure.persistence.entity.flow;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Getter @Setter
@Entity @Table(name = "flow_message")
@SQLRestriction("Visible = 1")
public class FlowMessageEntity extends BaseEntity {
    @Column(name = "Code", nullable = false, length = 60) private String code;
    @Column(name = "Name", nullable = false, length = 120) private String name;
    @Column(name = "Body", nullable = false, length = 1000) private String body;
    @Column(name = "Description", length = 300) private String description;
}
