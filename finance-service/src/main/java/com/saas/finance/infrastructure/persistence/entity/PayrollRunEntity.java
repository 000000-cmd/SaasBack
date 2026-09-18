package com.saas.finance.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "payroll_run")
@SQLRestriction("Visible = 1")
public class PayrollRunEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BranchId", length = 36)
    private UUID branchId;

    @Column(name = "Code", length = 32, nullable = false) private String code;
    @Column(name = "PeriodLabel", length = 60, nullable = false) private String periodLabel;
    @Column(name = "PeriodStart", nullable = false) private LocalDate periodStart;
    @Column(name = "PeriodEnd", nullable = false) private LocalDate periodEnd;
    @Column(name = "EmployeeCount", nullable = false) private Integer employeeCount;
    @Column(name = "TotalAmount", precision = 14, scale = 2, nullable = false) private BigDecimal totalAmount;
    @Column(name = "Currency", length = 3, nullable = false) private String currency;
    @Column(name = "Status", length = 16, nullable = false) private String status;
    @Column(name = "ExecutedAt", nullable = false) private LocalDateTime executedAt;
    @Column(name = "VoidedAt") private LocalDateTime voidedAt;
    @Column(name = "VoidReason", length = 300) private String voidReason;
    @Column(name = "Note", length = 255) private String note;
    @Column(name = "IdempotencyKey", length = 64) private String idempotencyKey;
}
