package com.saas.system.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Entidad JPA para definiciones de listas del sistema.
 * Esta tabla almacena los metadatos de qué listas existen y en qué tabla física se almacenan.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_list_definition")
@EqualsAndHashCode(callSuper = true)
public class ListDefinitionEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    /**
     * Nombre para mostrar de la lista.
     * Ejemplo: "Tipos de Documento", "Tipos de Género"
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * Nombre de la tabla física en la base de datos.
     * Ejemplo: sys_list_document_types, sys_list_gender_types
     */
    @Column(name = "physical_table_name", nullable = false, unique = true, length = 100)
    private String physicalTableName;
}