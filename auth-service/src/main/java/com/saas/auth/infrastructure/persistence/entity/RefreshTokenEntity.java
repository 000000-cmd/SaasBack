package com.saas.auth.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Refresh token persistente.
 *
 * Hereda Id (UUID), Enabled, Visible, AuditUser, AuditDate, CreatedDate.
 * Adicional: UserId (FK), Token (string opaco), ExpiresAt, RevokedAt (nullable).
 *
 * Para logout / blacklist usamos Redis con TTL = remaining lifetime, mas barato
 * que actualizar este registro en cada cierre de sesion.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_token")
public class RefreshTokenEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_refresh_token_user"))
    private UserEntity user;

    @Column(name = "Token", nullable = false, length = 500)
    private String token;

    @Column(name = "ExpiresAt", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "RevokedAt")
    private LocalDateTime revokedAt;
}
