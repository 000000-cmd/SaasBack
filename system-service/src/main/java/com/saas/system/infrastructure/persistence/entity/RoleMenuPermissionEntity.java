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
@Table(name = "sys_rolemenupermission", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"RoleMenuId", "PermissionId"})
})
@EqualsAndHashCode(callSuper = true, exclude = {"RoleMenu", "Permission"})
@ToString(exclude = {"RoleMenu", "Permission"})
public class RoleMenuPermissionEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "Id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoleMenuId", nullable = false)
    private RoleMenuEntity roleMenu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PermissionId", nullable = false)
    private PermissionEntity permission;
}