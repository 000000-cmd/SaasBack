package com.saas.common.mapper;

import java.util.List;

/**
 * Interface base para mappers entre Domain y Entity.
 * Usar con MapStruct.
 *
 * @param <D> Tipo del modelo de Dominio
 * @param <E> Tipo de la Entidad JPA
 */
public interface IBaseMapper<D, E> {

    /**
     * Convierte una entidad JPA a modelo de dominio
     */
    D toDomain(E entity);

    /**
     * Convierte un modelo de dominio a entidad JPA
     */
    E toEntity(D domain);

    /**
     * Convierte una lista de entidades a lista de dominios
     */
    List<D> toDomainList(List<E> entities);

    /**
     * Convierte una lista de dominios a lista de entidades
     */
    List<E> toEntityList(List<D> domains);
}