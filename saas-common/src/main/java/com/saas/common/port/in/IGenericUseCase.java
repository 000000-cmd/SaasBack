package com.saas.common.port.in;

import com.saas.common.exception.DuplicateResourceException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.model.BaseDomain;
import com.saas.common.model.IBusinessEntity;

import java.util.List;

/**
 * Interface genérica para casos de uso CRUD (Input Port).
 * Define las operaciones de negocio básicas.
 *
 * @param <T> Tipo del modelo de dominio
 * @param <ID> Tipo del identificador
 */
public interface IGenericUseCase<T extends BaseDomain & IBusinessEntity<ID>, ID> {

    /**
     * Crea una nueva entidad
     * @throws DuplicateResourceException si ya existe con el mismo código
     */
    T create(T entity);

    /**
     * Actualiza una entidad existente
     * @throws ResourceNotFoundException si no existe
     */
    T update(ID id, T entity);

    /**
     * Obtiene una entidad por su código
     * @throws ResourceNotFoundException si no existe
     */
    T getByCode(String code);

    /**
     * Obtiene una entidad por su ID
     * @throws ResourceNotFoundException si no existe
     */
    T getById(ID id);

    /**
     * Obtiene todas las entidades visibles
     */
    List<T> getAll();

    /**
     * Elimina una entidad por ID (soft delete)
     * @throws ResourceNotFoundException si no existe
     */
    void delete(ID id);

    /**
     * Activa o desactiva una entidad
     * @throws ResourceNotFoundException si no existe
     */
    void toggleEnabled(ID id, boolean enabled);
}