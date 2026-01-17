package com.saas.system.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.IBusinessEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modelo de dominio para Roles del sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseDomain implements IBusinessEntity<String> {

    private String id;
    private String code;
    private String name;
    private String description;
    private Integer displayOrder;
}