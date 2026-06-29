package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "business_offering")
@SQLRestriction("Visible = 1")
public class OfferingEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "CategoryId", length = 36)
    private UUID categoryId;
    @Column(name = "Name", length = 160, nullable = false) private String name;
    @Column(name = "Description", length = 500) private String description;
    @Column(name = "DurationMinutes", nullable = false) private Integer durationMinutes;
    @Column(name = "Price", precision = 12, scale = 2, nullable = false) private BigDecimal price;
    @Column(name = "IsActive", nullable = false) private Boolean isActive = Boolean.TRUE;
}
