package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Entidad JPA para Constantes.
 */
@Data
@Entity
@Table(name = "sys_constant")
@EqualsAndHashCode(callSuper = true)
public class ConstantEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "Id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "Code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "Value", nullable = false, length = 500)
    private String value;

    @Column(name = "Description")
    private String description;

    @Column(name = "Category", length = 100)
    private String category;
}
