package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "specialty")
@SQLRestriction("Visible = 1")
public class SpecialtyEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @Column(name = "Name", length = 120, nullable = false) private String name;
    @Column(name = "DisplayOrder", nullable = false) private Integer displayOrder = 0;
}
