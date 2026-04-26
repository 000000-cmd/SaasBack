package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catalogo configurable (lista del sistema).
 * Ejemplos: TIPOS_DOCUMENTO, ESTADOS_REGISTRO, GENEROS.
 * Sus items viven en {@link SystemListItemEntity}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "system_list")
public class SystemListEntity extends BaseEntity {

    @Column(name = "Code", nullable = false, length = 80)
    private String code;

    @Column(name = "Name", nullable = false, length = 120)
    private String name;

    @Column(name = "Description", length = 500)
    private String description;
}
