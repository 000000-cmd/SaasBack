package com.saas.common.port.in;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.model.BaseDomain;

import java.util.List;

/**
 * Caso de uso CRUD generico (basado en Id).
 *
 * @param <T>  tipo de dominio
 * @param <ID> tipo del identificador
 */
public interface IGenericUseCase<T extends BaseDomain, ID> {

    T create(T entity);

    /**
     * Actualiza solo los campos de negocio mutables. La implementacion concreta
     * decide que campos se pueden modificar via el callback de merge.
     *
     * @throws ResourceNotFoundException si no existe
     */
    T update(ID id, T entity);

    /**
     * @throws ResourceNotFoundException si no existe
     */
    T getById(ID id);

    List<T> getAll();

    /**
     * Soft delete (Enabled = Visible = false).
     *
     * @throws ResourceNotFoundException si no existe
     */
    void delete(ID id);

    /**
     * Habilita/deshabilita una entidad sin afectar visibilidad.
     *
     * @throws ResourceNotFoundException si no existe
     */
    void toggleEnabled(ID id, boolean enabled);
}
