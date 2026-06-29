package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "employee_compensation")
@SQLRestriction("Visible = 1")
public class EmployeeCompensationEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "EmployeeId", length = 36, nullable = false)
    private UUID employeeId;
    @Column(name = "CompensationType", length = 40, nullable = false) private String compensationType;
    @Column(name = "BaseSalary", precision = 12, scale = 2) private BigDecimal baseSalary;
    @Column(name = "ServicePercentage", precision = 5, scale = 2) private BigDecimal servicePercentage;
    @Column(name = "FixedCommission", precision = 12, scale = 2) private BigDecimal fixedCommission;
    @Column(name = "ValidFrom", nullable = false) private LocalDateTime validFrom;
    @Column(name = "ValidTo") private LocalDateTime validTo;
}
