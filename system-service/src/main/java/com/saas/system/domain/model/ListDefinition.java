package com.saas.system.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modelo de dominio para la definición de listas del sistema.
 * Define qué listas existen y en qué tabla física se almacenan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ListDefinition extends BaseDomain {

    private String id;

    /**
     * Nombre para mostrar de la lista.
     * Ejemplo: "Tipos de Documento", "Tipos de Género", "Tipos de Rol"
     */
    private String displayName;

    /**
     * Nombre de la tabla física en la base de datos.
     * Ejemplo: sys_list_document_types, sys_list_gender_types
     */
    private String physicalTableName;
}