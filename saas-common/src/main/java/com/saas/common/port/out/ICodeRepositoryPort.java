package com.saas.common.port.out;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ICodeable;

import java.util.Optional;

/**
 * Puerto de salida para entidades que tienen {@code Code} unico de negocio.
 * Aniade busqueda por codigo al CRUD generico.
 */
public interface ICodeRepositoryPort<T extends BaseDomain & ICodeable, ID>
        extends IGenericRepositoryPort<T, ID> {

    Optional<T> findByCode(String code);

    boolean existsByCode(String code);
}
