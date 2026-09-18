package com.saas.auth.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila de {@code user_device_link}. El porque de cada columna y de los dos
 * indices esta en V3__1.0.0.sql.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_device_link")
public class UserDeviceLinkEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false,
            foreignKey = @ForeignKey(name = "fk_udl_user"))
    private UserEntity user;

    @Column(name = "DeviceId", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "DeviceName", length = 120)
    private String deviceName;

    @Column(name = "Platform", nullable = false, length = 16)
    private String platform;

    @Column(name = "AppVersion", length = 32)
    private String appVersion;

    @Column(name = "LastSeenAt", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "RevokedAt")
    private LocalDateTime revokedAt;

    // CHAR(36) como TODOS los UUID del proyecto (ver BaseEntity). Sin esto,
    // Hibernate 6 mapea un UUID a binary(16) por defecto y la validacion de
    // esquema tumba el arranque del servicio entero.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "RevokedBy", length = 36)
    private UUID revokedBy;
}
