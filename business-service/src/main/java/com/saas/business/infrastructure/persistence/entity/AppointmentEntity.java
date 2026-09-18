package com.saas.business.infrastructure.persistence.entity;

import com.saas.business.domain.model.AppointmentChannel;
import com.saas.business.domain.model.AppointmentStatus;
import com.saas.business.domain.model.CancelledBy;
import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Fila de {@code appointment}. El porque de cada columna esta en V1. */
@Getter
@Setter
@Entity
@Table(name = "appointment")
@SQLRestriction("Visible = 1")
public class AppointmentEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BranchId", length = 36, nullable = false)
    private UUID branchId;

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "EmployeeId", length = 36, nullable = false)
    private UUID employeeId;

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessClientId", length = 36, nullable = false)
    private UUID businessClientId;

    @Enumerated(EnumType.STRING) @Column(name = "Channel", length = 16, nullable = false)
    private AppointmentChannel channel;

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "CreatedByUserId", length = 36)
    private UUID createdByUserId;

    @Column(name = "StartUtc", nullable = false)
    private Instant startUtc;

    @Column(name = "EndUtc", nullable = false)
    private Instant endUtc;

    @Column(name = "BusinessTimeZone", length = 64, nullable = false)
    private String businessTimeZone;

    @Column(name = "LocalDate", nullable = false)
    private LocalDate localDate;

    @Enumerated(EnumType.STRING) @Column(name = "Status", length = 28, nullable = false)
    private AppointmentStatus status;

    /**
     * Bloqueo optimista de verdad: lo gestiona Hibernate. Dos pantallas
     * editando la misma cita hacen que la segunda falle en vez de pisar.
     */
    @Version @Column(name = "Version", nullable = false)
    private Integer version;

    @Column(name = "PublicCode", length = 12, nullable = false)
    private String publicCode;

    @Column(name = "IsBackdated", nullable = false)
    private Boolean backdated = Boolean.FALSE;

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "RescheduledFromAppointmentId", length = 36)
    private UUID rescheduledFromAppointmentId;

    @Column(name = "CancelReason", length = 300)
    private String cancelReason;

    @Enumerated(EnumType.STRING) @Column(name = "CancelledBy", length = 16)
    private CancelledBy cancelledBy;

    @Column(name = "CancelledAt")
    private Instant cancelledAt;

    @Column(name = "StartedAt")
    private Instant startedAt;

    @Column(name = "CompletedAt")
    private Instant completedAt;

    @Column(name = "TotalPrice", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "TotalDurationMinutes", nullable = false)
    private Integer totalDurationMinutes = 0;

    @Column(name = "Currency", length = 3, nullable = false)
    private String currency = "COP";

    @Column(name = "Notes", length = 500)
    private String notes;
}
