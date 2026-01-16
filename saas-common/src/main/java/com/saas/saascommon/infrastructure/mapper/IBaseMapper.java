package com.saas.saascommon.infrastructure.mapper;

import java.util.List;

public interface IBaseMapper<D, E> {

    D toDomain(E entity);

    E toEntity(D domain);

    List<D> toDomainList(List<E> entities);

    List<E> toEntityList(List<D> domains);
}
