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

import java.time.LocalDateTime;
import java.util.UUID;

/** Fila de {@code appointment_notification}. Solo se anade, nunca se modifica. */
@Getter
@Setter
@Entity
@Table(name = "appointment_notification")
@SQLRestriction("Visible = 1")
public class AppointmentNoticeEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "AppointmentId", length = 36, nullable = false)
    private UUID appointmentId;

    @Column(name = "NotificationCode", length = 80, nullable = false)
    private String notificationCode;

    @Column(name = "SentAt", nullable = false)
    private LocalDateTime sentAt;
}
