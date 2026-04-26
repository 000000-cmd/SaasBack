package com.saas.common.port.out;

import com.saas.common.model.BaseDomain;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida generico (CRUD basado en Id).
 * Lo usan todas las entidades, incluyendo las tablas puente.
 *
 * @param <T>  tipo de dominio
 * @param <ID> tipo del identificador
 */
public interface IGenericRepositoryPort<T extends BaseDomain, ID> {

    T save(T entity);

    /**
     * Actualiza una entidad existente. La implementacion debe cargar la entidad
     * actual y hacer merge para preservar campos inmutables como {@code CreatedDate}.
     */
    T update(T entity);

    Optional<T> findById(ID id);

    boolean existsById(ID id);

    List<T> findAll();

    /**
     * Soft-delete: marca {@code Enabled = false} y {@code Visible = false}.
     */
    void softDeleteById(ID id);

    /**
     * Hard-delete: elimina fisicamente el registro. Usar solo cuando no haya
     * dependencias o sea estrictamente necesario.
     */
    void hardDeleteById(ID id);
}
