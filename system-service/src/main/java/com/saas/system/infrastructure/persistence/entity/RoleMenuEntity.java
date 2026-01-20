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
@Table(name = "sys_rolemenu", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"RoleId", "MenuId"})
})
@EqualsAndHashCode(callSuper = true, exclude = {"Role", "Menu"})
@ToString(exclude = {"Role", "Menu"})
public class RoleMenuEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "Id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoleId", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MenuId", nullable = false)
    private MenuEntity menu;
}