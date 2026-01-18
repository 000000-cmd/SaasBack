package com.saas.system.domain.port.out;

import com.saas.system.domain.model.DynamicList;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de items de listas dinámicas.
 * Opera sobre tablas dinámicas usando el nombre de tabla como parámetro.
 *
 * NOTA: Esta interface NO extiende IGenericRepositoryPort porque
 * sus métodos requieren tableName como primer parámetro.
 */
public interface IDynamicListRepositoryPort {

    /**
     * Guarda un item en una lista
     */
    DynamicList save(String tableName, DynamicList item);

    /**
     * Actualiza un item en una lista
     */
    DynamicList update(String tableName, DynamicList item);

    /**
     * Obtiene todos los items de una lista
     */
    List<DynamicList> findAll(String tableName);

    /**
     * Obtiene todos los items visibles de una lista
     */
    List<DynamicList> findAllVisible(String tableName);

    /**
     * Obtiene todos los items habilitados de una lista
     */
    List<DynamicList> findAllEnabled(String tableName);

    /**
     * Busca un item por ID
     */
    Optional<DynamicList> findById(String tableName, String id);

    /**
     * Busca un item por código
     */
    Optional<DynamicList> findByCode(String tableName, String code);

    /**
     * Verifica si existe un item con el código dado
     */
    boolean existsByCode(String tableName, String code);

    /**
     * Verifica si existe un item con el ID dado
     */
    boolean existsById(String tableName, String id);

    /**
     * Elimina un item (soft delete)
     */
    void softDelete(String tableName, String id);

    /**
     * Elimina permanentemente un item (hard delete)
     */
    void hardDelete(String tableName, String id);

    /**
     * Activa o desactiva un item
     */
    void toggleEnabled(String tableName, String id, boolean enabled);

    /**
     * Verifica si una tabla existe en la base de datos
     */
    boolean tableExists(String tableName);
}