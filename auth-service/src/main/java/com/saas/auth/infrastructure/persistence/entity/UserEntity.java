package com.saas.auth.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad JPA para Usuario.
 *
 * Tabla: {@code app_user} (prefijo {@code app_} porque {@code user} es reservada en MySQL).
 * Hereda Id, Enabled, Visible, AuditUser, AuditDate, CreatedDate de {@link BaseEntity}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "app_user")
public class UserEntity extends BaseEntity {

    @Column(name = "Username", nullable = false, length = 60)
    private String username;

    @Column(name = "Email", nullable = false, length = 120)
    private String email;

    @Column(name = "PasswordHash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "FirstName", nullable = false, length = 80)
    private String firstName;

    @Column(name = "LastName", nullable = false, length = 80)
    private String lastName;

    @Column(name = "ProfilePhoto", length = 500)
    private String profilePhoto;

    @Column(name = "Theme", nullable = false, length = 30)
    private String theme;

    @Column(name = "LanguageCode", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "LastLoginAt")
    private LocalDateTime lastLoginAt;
}
