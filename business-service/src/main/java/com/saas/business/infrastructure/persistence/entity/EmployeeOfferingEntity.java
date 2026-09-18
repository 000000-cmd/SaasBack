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
@Entity @Table(name = "employee_offering")
@SQLRestriction("Visible = 1")
public class EmployeeOfferingEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "EmployeeId", length = 36, nullable = false)
    private UUID employeeId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "OfferingId", length = 36, nullable = false)
    private UUID offeringId;
    @Column(name = "DurationMinutes") private Integer durationMinutes;
    @Column(name = "CommissionRate", precision = 5, scale = 2) private BigDecimal commissionRate;
}
