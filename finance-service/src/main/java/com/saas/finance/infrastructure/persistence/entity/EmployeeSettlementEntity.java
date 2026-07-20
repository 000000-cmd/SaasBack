package com.saas.finance.infrastructure.persistence.entity;

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
@Entity @Table(name = "employee_settlement")
@SQLRestriction("Visible = 1")
public class EmployeeSettlementEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BranchId", length = 36)
    private UUID branchId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "EmployeeId", length = 36, nullable = false)
    private UUID employeeId;
    @Column(name = "Amount", precision = 14, scale = 2, nullable = false) private BigDecimal amount;
    @Column(name = "BalanceBefore", precision = 14, scale = 2, nullable = false) private BigDecimal balanceBefore;
    @Column(name = "Currency", length = 3, nullable = false) private String currency;
    @Column(name = "SettledAt", nullable = false) private LocalDateTime settledAt;
    @Column(name = "Note", length = 255) private String note;
}
