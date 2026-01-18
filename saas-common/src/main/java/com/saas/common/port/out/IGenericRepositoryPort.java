package com.saas.common.port.out;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.IBusinessEntity;

import java.util.List;
import java.util.Optional;

/**
 * Interface genérica para puertos de salida (repositorios).
 * Define SOLO las operaciones que usa GenericCrudService.
 *
 * Las interfaces específicas (IRoleRepositoryPort, etc.) deben extender
 * esta interface y agregar los métodos adicionales que necesiten.
 *
 * @param <T> Tipo del modelo de dominio
 * @param <ID> Tipo del identificador
 */
public interface IGenericRepositoryPort<T extends BaseDomain & IBusinessEntity<ID>, ID> {

    /**
     * Guarda una nueva entidad
     */
    T save(T entity);

    /**
     * Actualiza una entidad existente
     */
    T update(T entity);

    /**
     * Busca una entidad por su ID
     */
    Optional<T> findById(ID id);

    /**
     * Busca una entidad por su código de negocio
     */
    Optional<T> findByCode(String code);

    /**
     * Verifica si existe una entidad con el código dado
     */
    boolean existsByCode(String code);

    /**
     * Verifica si existe una entidad con el ID dado
     */
    boolean existsById(ID id);

    /**
     * Obtiene todas las entidades visibles
     */
    List<T> findAll();

    /**
     * Elimina una entidad por su ID (soft delete)
     */
    void deleteById(ID id);
}