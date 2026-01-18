package com.saas.system.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.Role;

import java.util.List;

/**
 * Puerto de salida para persistencia de Roles.
 * Extiende IGenericRepositoryPort para operaciones CRUD básicas
 * y agrega métodos específicos.
 */
public interface IRoleRepositoryPort extends IGenericRepositoryPort<Role, String> {

    /**
     * Obtiene todas las entidades incluyendo las no visibles
     */
    List<Role> findAllIncludingHidden();

    /**
     * Elimina permanentemente una entidad (hard delete)
     */
    void hardDeleteById(String id);

    /**
     * Cuenta las entidades visibles
     */
    long count();
}