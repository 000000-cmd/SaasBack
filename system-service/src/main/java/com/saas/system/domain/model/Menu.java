package com.saas.system.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ICodeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Menu jerarquico configurable.
 *   - Sin {@code parentId}  -> seccion principal (top-level).
 *   - Con {@code parentId}  -> sub-seccion del menu padre.
 *
 * La visibilidad por usuario se calcula en runtime intersectando los roles
 * del usuario con las relaciones {@code menu_role}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Menu extends BaseDomain implements ICodeable {

    private String code;
    private String name;
    private String icon;
    private String route;
    private UUID parentId;
    private Integer displayOrder;

    /**
     * Marca el menu como submenu: en vez de ocupar un carril del menu lateral,
     * se dibuja en el nav secundario de su padre. Exige {@code parentId} — un
     * submenu sin padre no tiene donde pintarse.
     */
    private Boolean submenu;

    public boolean isRoot() {
        return parentId == null;
    }

    public boolean submenu() {
        return Boolean.TRUE.equals(submenu);
    }
}
