package com.saas.events.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.events.domain.model.UserDevice;
import com.saas.events.infrastructure.persistence.entity.UserDeviceEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface UserDevicePersistenceMapper extends IBaseMapper<UserDevice, UserDeviceEntity> {
}
