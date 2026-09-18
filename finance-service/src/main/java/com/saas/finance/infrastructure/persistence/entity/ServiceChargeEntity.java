package com.saas.finance.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import com.saas.finance.domain.model.ChargeStatus;
import com.saas.finance.domain.model.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "service_charge")
@SQLRestriction("Visible = 1")
public class ServiceChargeEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BranchId", length = 36)
    private UUID branchId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "EmployeeId", length = 36, nullable = false)
    private UUID employeeId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "AppointmentId", length = 36)
    private UUID appointmentId;

    @Column(name = "ServiceName", length = 160, nullable = false) private String serviceName;
    @Column(name = "ServiceDate", nullable = false) private LocalDate serviceDate;
    @Column(name = "StartTime") private LocalTime startTime;
    @Column(name = "EndTime") private LocalTime endTime;

    @Column(name = "ClientName", length = 160) private String clientName;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "ClientThirdPartyId", length = 36)
    private UUID clientThirdPartyId;
    @Column(name = "ClientEmail", length = 150) private String clientEmail;
    @Column(name = "ClientPhone", length = 30) private String clientPhone;

    @Column(name = "GrossAmount", precision = 14, scale = 2, nullable = false) private BigDecimal grossAmount;
    @Column(name = "DeductionRate", precision = 5, scale = 2, nullable = false) private BigDecimal deductionRate;
    @Column(name = "DeductionAmount", precision = 14, scale = 2, nullable = false) private BigDecimal deductionAmount;
    @Column(name = "NetAmount", precision = 14, scale = 2, nullable = false) private BigDecimal netAmount;
    @Column(name = "Currency", length = 3, nullable = false) private String currency;

    @Enumerated(EnumType.STRING) @Column(name = "PaymentMethod", length = 16, nullable = false)
    private PaymentMethod paymentMethod;
    @Column(name = "ReceiptUrl", length = 500) private String receiptUrl;
    @Column(name = "ResultPhotoUrl", length = 500) private String resultPhotoUrl;

    @Enumerated(EnumType.STRING) @Column(name = "Status", length = 16, nullable = false)
    private ChargeStatus status;
    @Column(name = "ConfirmedAt") private LocalDateTime confirmedAt;
    @Column(name = "DiscardedAt") private LocalDateTime discardedAt;
    @Column(name = "DiscardReason", length = 300) private String discardReason;

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "SettlementId", length = 36)
    private UUID settlementId;
}
