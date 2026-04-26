package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "role_permission",
        uniqueConstraints = @UniqueConstraint(name = "uq_role_permission", columnNames = {"RoleId", "PermissionId"})
)
public class RolePermissionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RoleId", nullable = false, foreignKey = @ForeignKey(name = "fk_role_permission_role"))
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PermissionId", nullable = false, foreignKey = @ForeignKey(name = "fk_role_permission_permission"))
    private PermissionEntity permission;
}
