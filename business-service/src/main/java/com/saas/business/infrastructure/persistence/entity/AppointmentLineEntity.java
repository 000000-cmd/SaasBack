package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/** Fila de {@code appointment_service}: un servicio de la cita, congelado. */
@Getter
@Setter
@Entity
@Table(name = "appointment_service")
@SQLRestriction("Visible = 1")
public class AppointmentLineEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "AppointmentId", length = 36, nullable = false)
    private UUID appointmentId;

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "OfferingId", length = 36)
    private UUID offeringId;

    @Column(name = "ServiceName", length = 160, nullable = false)
    private String serviceName;

    @Column(name = "Price", precision = 14, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "DurationMinutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "CommissionRate", precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(name = "CommissionAmount", precision = 14, scale = 2, nullable = false)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "DisplayOrder", nullable = false)
    private Integer displayOrder = 0;
}
