package com.saas.auth.infrastructure.persistence.mapper;

import com.saas.auth.domain.model.UserDeviceLink;
import com.saas.auth.infrastructure.persistence.entity.UserDeviceLinkEntity;
import com.saas.auth.infrastructure.persistence.entity.UserEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(config = BaseMapStructConfig.class)
public interface UserDeviceLinkPersistenceMapper
        extends IBaseMapper<UserDeviceLink, UserDeviceLinkEntity> {

    @Override
    @Mapping(target = "userId", source = "user.id")
    UserDeviceLink toDomain(UserDeviceLinkEntity entity);

    @Override
    @Mapping(target = "user", source = "userId", qualifiedByName = "userRefFromId")
    UserDeviceLinkEntity toEntity(UserDeviceLink domain);

    @Named("userRefFromId")
    default UserEntity userRefFromId(UUID userId) {
        if (userId == null) return null;
        UserEntity ref = new UserEntity();
        ref.setId(userId);
        return ref;
    }
}
