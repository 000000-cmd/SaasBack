package com.saas.common.mapper;

import java.util.List;

/**
 * Interface para mappers de Domain a Response DTO.
 * Usar con MapStruct.
 *
 * @param <D> Tipo del modelo de Dominio
 * @param <S> Tipo del Response DTO
 */
public interface IResponseMapper<D, S> {

    /**
     * Convierte un modelo de dominio a Response DTO
     */
    S toResponse(D domain);

    /**
     * Convierte una lista de dominios a lista de Response DTOs
     */
    List<S> toResponseList(List<D> domains);
}