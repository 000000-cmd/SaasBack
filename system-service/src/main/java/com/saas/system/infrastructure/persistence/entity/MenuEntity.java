package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Entidad JPA para Menús.
 */
@Data
@Entity
@Table(name = "sys_menu")
@EqualsAndHashCode(callSuper = true)
public class MenuEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "router_link")
    private String routerLink;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "parent_id", columnDefinition = "VARCHAR(36)")
    private UUID parentId;
}