package com.saas.auth.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.auth.domain.model.UserRole;
import com.saas.auth.infrastructure.persistence.entity.UserEntity;
import com.saas.auth.infrastructure.persistence.entity.UserRoleEntity;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = BaseMapStructConfig.class)
public interface UserRolePersistenceMapper extends IBaseMapper<UserRole, UserRoleEntity> {

    /** Aplana entity.user.id -> domain.userId. */
    @Override
    @Mapping(target = "userId", source = "user.id")
    UserRole toDomain(UserRoleEntity entity);

    /** Construye un UserEntity stub solo con Id para no perder la FK. */
    @Override
    @Mapping(target = "user", source = "userId", qualifiedByName = "userRefFromId")
    UserRoleEntity toEntity(UserRole domain);

    @Named("userRefFromId")
    default UserEntity userRefFromId(java.util.UUID userId) {
        if (userId == null) return null;
        UserEntity ref = new UserEntity();
        ref.setId(userId);
        return ref;
    }
}
