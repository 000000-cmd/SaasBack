package com.saas.auth.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.auth.domain.model.User;
import com.saas.auth.infrastructure.persistence.entity.UserEntity;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapStructConfig.class)
public interface UserPersistenceMapper extends IBaseMapper<User, UserEntity> {

    /** roleCodes es transient en User; no existe en UserEntity. */
    @Override
    @Mapping(target = "roleCodes", ignore = true)
    User toDomain(UserEntity entity);

    @Override
    UserEntity toEntity(User domain);
}
