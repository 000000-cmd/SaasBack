package com.saas.system.domain.port.in;

import com.saas.common.port.in.IGenericUseCase;
import com.saas.system.domain.model.Menu;

import java.util.List;

/**
 * Puerto de entrada (caso de uso) para Menús.
 */
public interface IMenuUseCase extends IGenericUseCase<Menu, String> {

    /**
     * Obtiene los menús hijos de un menú padre
     */
    List<Menu> getByParentId(String parentId);

    /**
     * Obtiene los menús raíz (sin padre)
     */
    List<Menu> getRootMenus();
}
