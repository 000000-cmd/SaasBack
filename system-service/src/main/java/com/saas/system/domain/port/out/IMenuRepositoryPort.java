package com.saas.system.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.Menu;

import java.util.List;

/**
 * Puerto de salida para persistencia de Menús.
 * Extiende IGenericRepositoryPort para operaciones CRUD básicas
 * y agrega métodos específicos.
 */
public interface IMenuRepositoryPort extends IGenericRepositoryPort<Menu, String> {

    /**
     * Obtiene todas las entidades incluyendo las no visibles
     */
    List<Menu> findAllIncludingHidden();

    /**
     * Elimina permanentemente una entidad (hard delete)
     */
    void hardDeleteById(String id);

    /**
     * Cuenta las entidades visibles
     */
    long count();

    /**
     * Obtiene los menús hijos de un menú padre
     */
    List<Menu> findByParentId(String parentId);

    /**
     * Obtiene los menús raíz (sin padre)
     */
    List<Menu> findRootMenus();
}