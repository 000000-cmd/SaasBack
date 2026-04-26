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

    public boolean isRoot() {
        return parentId == null;
    }
}
