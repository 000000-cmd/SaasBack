package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Entidad JPA para asignación de Menús a Roles.
 */
@Data
@Entity
@Table(name = "sys_role_menu", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"role_id", "menu_id"})
})
@EqualsAndHashCode(callSuper = true, exclude = {"role", "menu"})
@ToString(exclude = {"role", "menu"})
public class RoleMenuEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private MenuEntity menu;
}