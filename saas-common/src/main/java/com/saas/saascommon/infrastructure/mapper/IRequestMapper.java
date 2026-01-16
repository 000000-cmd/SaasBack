package com.saas.saascommon.infrastructure.mapper;

public interface IRequestMapper<D, R> {
    D toDomain(R request);
}
