package com.saas.business.infrastructure.persistence.entity;

import com.saas.business.domain.model.AppointmentChannel;
import com.saas.business.domain.model.AppointmentStatus;
import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Fila de {@code appointment_history}. Solo se anade, nunca se modifica. */
@Getter
@Setter
@Entity
@Table(name = "appointment_history")
@SQLRestriction("Visible = 1")
public class AppointmentHistoryEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "AppointmentId", length = 36, nullable = false)
    private UUID appointmentId;

    @Enumerated(EnumType.STRING) @Column(name = "FromStatus", length = 28)
    private AppointmentStatus fromStatus;

    @Enumerated(EnumType.STRING) @Column(name = "ToStatus", length = 28, nullable = false)
    private AppointmentStatus toStatus;

    @Enumerated(EnumType.STRING) @Column(name = "Channel", length = 16, nullable = false)
    private AppointmentChannel channel;

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "ActorUserId", length = 36)
    private UUID actorUserId;

    @Column(name = "Reason", length = 300)
    private String reason;

    @Column(name = "OccurredAt", nullable = false)
    private Instant occurredAt;
}
