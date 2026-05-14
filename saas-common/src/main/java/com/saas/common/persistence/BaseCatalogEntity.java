package com.saas.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Superclase para entidades JPA de catalogos.
 *
 * Todos los catalogos comparten la misma estructura: Code (negocio unico),
 * Name (legible), Value (opcional para mapeos a constantes), DisplayOrder
 * (para ordenamiento en UI). Las subclases solo declaran {@code @Entity} y
 * {@code @Table(name="...")}.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseCatalogEntity extends BaseEntity {

    @Column(name = "Code", nullable = false, length = 80)
    private String code;

    @Column(name = "Name", nullable = false, length = 120)
    private String name;

    @Column(name = "Value", length = 500)
    private String value;

    @Column(name = "DisplayOrder", nullable = false)
    private Integer displayOrder = 0;
}
