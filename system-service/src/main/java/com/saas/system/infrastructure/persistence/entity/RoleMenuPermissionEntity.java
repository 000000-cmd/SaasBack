package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Entidad JPA para permisos de RoleMenu.
 */
@Data
@Entity
@Table(name = "sys_role_menu_permission", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"role_menu_id", "permission_id"})
})
@EqualsAndHashCode(callSuper = true, exclude = {"roleMenu", "permission"})
@ToString(exclude = {"roleMenu", "permission"})
public class RoleMenuPermissionEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_menu_id", nullable = false)
    private RoleMenuEntity roleMenu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionEntity permission;
}