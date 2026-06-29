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
@Entity @Table(name = "branch_offering")
@SQLRestriction("Visible = 1")
public class BranchOfferingEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BranchId", length = 36, nullable = false)
    private UUID branchId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "OfferingId", length = 36)
    private UUID offeringId;
    @Column(name = "Name", length = 160) private String name;
    @Column(name = "Description", length = 500) private String description;
    @Column(name = "DurationMinutes") private Integer durationMinutes;
    @Column(name = "Price", precision = 12, scale = 2) private BigDecimal price;
    @Column(name = "IsEnabled", nullable = false) private Boolean isEnabled = Boolean.TRUE;
    @Column(name = "IsActive", nullable = false) private Boolean isActive = Boolean.TRUE;
}
