package com.saas.system.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modelo de dominio para items de listas dinámicas del sistema.
 * Todas las listas comparten la misma estructura: code, name, order, enabled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DynamicList extends BaseDomain {

    private String id;
    private String code;
    private String name;
    private Integer displayOrder;

    /**
     * Nombre de la tabla física donde se almacena esta lista.
     * Ejemplo: sys_list_document_types, sys_list_gender_types, etc.
     */
    private String listType;
}
