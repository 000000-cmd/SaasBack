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
@Table(name = "sys_listdefinition")
@EqualsAndHashCode(callSuper = true)
public class ListDefinitionEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "Id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "DisplayName", nullable = false, length = 100)
    private String displayName;

    @Column(name = "PhysicalTableName", nullable = false, unique = true, length = 100)
    private String physicalTableName;
}