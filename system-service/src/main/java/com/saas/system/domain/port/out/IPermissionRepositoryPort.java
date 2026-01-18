package com.saas.system.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.Permission;

import java.util.List;

/**
 * Puerto de salida para persistencia de Permisos.
 * Extiende IGenericRepositoryPort para operaciones CRUD básicas
 * y agrega métodos específicos.
 */
public interface IPermissionRepositoryPort extends IGenericRepositoryPort<Permission, String> {

    /**
     * Obtiene todas las entidades incluyendo las no visibles
     */
    List<Permission> findAllIncludingHidden();

    /**
     * Elimina permanentemente una entidad (hard delete)
     */
    void hardDeleteById(String id);

    /**
     * Cuenta las entidades visibles
     */
    long count();
}