package com.saas.system.domain.port.out;

import com.saas.system.domain.model.ListDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de definiciones de listas.
 *
 * NOTA: Esta interface NO extiende IGenericRepositoryPort porque
 * ListDefinition no usa código (code) como identificador de negocio,
 * usa physicalTableName.
 */
public interface IListDefinitionRepositoryPort {

    /**
     * Guarda una definición de lista
     */
    ListDefinition save(ListDefinition definition);

    /**
     * Actualiza una definición de lista
     */
    ListDefinition update(ListDefinition definition);

    /**
     * Obtiene todas las definiciones visibles
     */
    List<ListDefinition> findAll();

    /**
     * Obtiene todas las definiciones incluyendo las no visibles
     */
    List<ListDefinition> findAllIncludingHidden();

    /**
     * Busca una definición por ID
     */
    Optional<ListDefinition> findById(String id);

    /**
     * Busca una definición por nombre de tabla física
     */
    Optional<ListDefinition> findByPhysicalTableName(String physicalTableName);

    /**
     * Verifica si existe una definición con el nombre de tabla dado
     */
    boolean existsByPhysicalTableName(String physicalTableName);

    /**
     * Verifica si existe una definición con el ID dado
     */
    boolean existsById(String id);

    /**
     * Elimina una definición por ID (soft delete)
     */
    void deleteById(String id);

    /**
     * Elimina permanentemente una definición (hard delete)
     */
    void hardDeleteById(String id);
}