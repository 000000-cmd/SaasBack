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
    @Column(name = "Id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "Code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "Label", nullable = false, length = 100)
    private String label;

    @Column(name = "RouterLink")
    private String routerLink;

    @Column(name = "Icon", length = 50)
    private String icon;

    @Column(name = "DisplayOrder")
    private Integer displayOrder;

    @Column(name = "ParentId", columnDefinition = "VARCHAR(36)")
    private UUID parentId;
}