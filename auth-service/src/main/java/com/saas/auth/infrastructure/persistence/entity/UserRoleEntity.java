package com.saas.auth.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Asignacion Usuario - Rol.
 *
 * El RoleId es FK logica a {@code role.Id} (tabla propiedad de system-service).
 * No se enforce constraint cross-service a nivel JPA aqui; la integridad la
 * valida el AuthService al asignar roles.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "user_role",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_role", columnNames = {"UserId", "RoleId"})
)
public class UserRoleEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_user_role_user"))
    private UserEntity user;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "RoleId", nullable = false, length = 36)
    private UUID roleId;
}
