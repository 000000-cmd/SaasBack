package com.saas.finance.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import com.saas.finance.domain.model.MovementType;
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

    @Enumerated(EnumType.STRING) @Column(name = "MovementType", length = 20, nullable = false)
    private MovementType movementType;
    @Column(name = "PeriodKey", length = 16) private String periodKey;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "PayrollRunId", length = 36)
    private UUID payrollRunId;
    @Column(name = "CommissionAmount", precision = 14, scale = 2, nullable = false) private BigDecimal commissionAmount;
    @Column(name = "BaseSalaryAmount", precision = 14, scale = 2, nullable = false) private BigDecimal baseSalaryAmount;
    @Column(name = "PayoutAccount", length = 120) private String payoutAccount;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BankAccountId", length = 36)
    private UUID bankAccountId;
    @Column(name = "PaymentProofUrl", length = 500) private String paymentProofUrl;
    // CHAR(64) y no VARCHAR: un SHA-256 en hexadecimal mide SIEMPRE 64. Se
    // declara el tipo porque si no la validacion de esquema al arrancar
    // reclama la diferencia y el servicio no levanta.
    @Column(name = "PaymentProofHash", columnDefinition = "char(64)")
    private String paymentProofHash;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "ReversalOfId", length = 36)
    private java.util.UUID reversalOfId;
    @Column(name = "PaidInCash", nullable = false) private Boolean paidInCash = Boolean.FALSE;
    @Column(name = "CashConfirmedAt") private LocalDateTime cashConfirmedAt;

    /**
     * Rellena las columnas NOT NULL que un abono no tiene por que conocer.
     *
     * <p>El valor por defecto del campo NO basta: el mapper copia el dominio
     * encima, y un abono (comision, sueldo base) llega con estos nulos porque
     * "pagado en efectivo" y "cuanto fue comision" son preguntas de un PAGO. Sin
     * esto, liquidar reventaba con "Column 'PaidInCash' cannot be null".</p>
     *
     * <p>Va en la entidad y no en cada builder para que valga tambien para el
     * proximo camino que cree un movimiento.</p>
     */
    @PrePersist
    @PreUpdate
    void defaults() {
        if (paidInCash == null) paidInCash = Boolean.FALSE;
        if (commissionAmount == null) commissionAmount = BigDecimal.ZERO;
        if (baseSalaryAmount == null) baseSalaryAmount = BigDecimal.ZERO;
    }
}
