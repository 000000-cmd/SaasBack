package com.saas.system.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.Constant;

import java.util.List;

/**
 * Puerto de salida para persistencia de Constantes.
 * Extiende IGenericRepositoryPort para operaciones CRUD básicas
 * y agrega métodos específicos.
 */
public interface IConstantRepositoryPort extends IGenericRepositoryPort<Constant, String> {

    /**
     * Obtiene todas las entidades incluyendo las no visibles
     */
    List<Constant> findAllIncludingHidden();

    /**
     * Elimina permanentemente una entidad (hard delete)
     */
    void hardDeleteById(String id);

    /**
     * Cuenta las entidades visibles
     */
    long count();

    /**
     * Obtiene constantes por categoría
     */
    List<Constant> findByCategory(String category);
}