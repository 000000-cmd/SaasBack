package com.saas.events.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.events.domain.model.NotificationParameter;
import com.saas.events.infrastructure.persistence.entity.NotificationParameterEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface NotificationParameterPersistenceMapper
        extends IBaseMapper<NotificationParameter, NotificationParameterEntity> {
}
