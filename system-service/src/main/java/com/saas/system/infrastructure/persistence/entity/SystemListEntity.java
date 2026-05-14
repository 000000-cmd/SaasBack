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
import org.hibernate.annotations.SQLRestriction;

/**
 * Meta-registro de catalogos del sistema.
 * Ejemplos: TIPOS_DOCUMENTO, ESTADOS_REGISTRO, GENEROS.
 * Los items de cada catalogo viven en su tabla propia
 * (document_type, registration_status, gender, ...) y se acceden via /list.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "system_list")
@SQLRestriction("Visible = 1")
public class SystemListEntity extends BaseEntity {

    @Column(name = "Code", nullable = false, length = 80)
    private String code;

    @Column(name = "Name", nullable = false, length = 120)
    private String name;

    @Column(name = "Description", length = 500)
    private String description;
}
