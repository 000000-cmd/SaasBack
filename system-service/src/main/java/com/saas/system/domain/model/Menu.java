package com.saas.system.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.IBusinessEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modelo de dominio para Menús del sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Menu extends BaseDomain implements IBusinessEntity<String> {

    private String id;
    private String code;
    private String label;
    private String routerLink;
    private String icon;
    private Integer displayOrder;
    private String parentId;

    /**
     * Indica si es un menú raíz (sin padre)
     */
    public boolean isRoot() {
        return parentId == null || parentId.isBlank();
    }
}