package com.saas.auth.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.auth.domain.model.RefreshToken;
import com.saas.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.saas.auth.infrastructure.persistence.entity.UserEntity;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = BaseMapStructConfig.class)
public interface RefreshTokenPersistenceMapper extends IBaseMapper<RefreshToken, RefreshTokenEntity> {

    @Override
    @Mapping(target = "userId", source = "user.id")
    RefreshToken toDomain(RefreshTokenEntity entity);

    @Override
    @Mapping(target = "user", source = "userId", qualifiedByName = "userRefFromId")
    RefreshTokenEntity toEntity(RefreshToken domain);

    @Named("userRefFromId")
    default UserEntity userRefFromId(java.util.UUID userId) {
        if (userId == null) return null;
        UserEntity ref = new UserEntity();
        ref.setId(userId);
        return ref;
    }
}
