package com.saas.auth.infrastructure.persistence.mapper;

import com.saas.auth.domain.model.RefreshToken;
import com.saas.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre RefreshToken (domain) y RefreshTokenEntity (JPA).
 */
@Mapper(componentModel = "spring")
public interface RefreshTokenPersistenceMapper {

    RefreshToken toDomain(RefreshTokenEntity entity);

    RefreshTokenEntity toEntity(RefreshToken domain);
}