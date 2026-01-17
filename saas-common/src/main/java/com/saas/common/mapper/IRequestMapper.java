package com.saas.common.mapper;

/**
 * Interface para mappers de Request DTO a Domain.
 * Usar con MapStruct.
 *
 * @param <D> Tipo del modelo de Dominio
 * @param <R> Tipo del Request DTO
 */
public interface IRequestMapper<D, R> {

    /**
     * Convierte un Request DTO a modelo de dominio
     */
    D toDomain(R request);
}