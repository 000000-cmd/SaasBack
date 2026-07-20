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
@Entity @Table(name = "employee_balance")
@SQLRestriction("Visible = 1")
public class EmployeeBalanceEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BranchId", length = 36)
    private UUID branchId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "EmployeeId", length = 36, nullable = false)
    private UUID employeeId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "ThirdPartyId", length = 36)
    private UUID thirdPartyId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "UserId", length = 36)
    private UUID userId;
    @Column(name = "AmountAccrued", precision = 14, scale = 2, nullable = false) private BigDecimal amountAccrued;
    @Column(name = "AmountPaid", precision = 14, scale = 2, nullable = false) private BigDecimal amountPaid;
    @Column(name = "Balance", precision = 14, scale = 2, nullable = false) private BigDecimal balance;
    @Column(name = "Currency", length = 3, nullable = false) private String currency;
    @Column(name = "LastCalculatedAt") private LocalDateTime lastCalculatedAt;
}
