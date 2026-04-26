package com.saas.common.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Contrato base para mappers Domain &lt;-&gt; Entity (implementacion via MapStruct).
 *
 * Convencion para los servicios concretos:
 *   - {@link #toDomain(Object)} y {@link #toEntity(Object)} cubren creacion y lectura.
 *   - {@link #updateEntityFromDomain(Object, Object)} cubre la actualizacion: hace merge
 *     del dominio entrante SOBRE la entidad existente preservando el Id y los campos de
 *     auditoria (los maneja {@code AuditingEntityListener}, NUNCA el dominio).
 *
 * @param <D> dominio
 * @param <E> entidad JPA
 */
public interface IBaseMapper<D, E> {

    D toDomain(E entity);

    E toEntity(D domain);

    List<D> toDomainList(List<E> entities);

    List<E> toEntityList(List<D> domains);

    /**
     * Merge del dominio entrante sobre una entidad existente.
     * - Se ignoran Id y campos de auditoria (el listener JPA los maneja).
     * - Los nulls del dominio NO sobrescriben (NullValuePropertyMappingStrategy.IGNORE),
     *   asi un PATCH parcial no borra datos existentes.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "auditUser",   ignore = true)
    @Mapping(target = "auditDate",   ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    void updateEntityFromDomain(D incoming, @MappingTarget E target);
}
