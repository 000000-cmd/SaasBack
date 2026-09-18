package com.saas.events.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.events.domain.model.Notification;
import com.saas.events.infrastructure.persistence.entity.NotificationEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface NotificationPersistenceMapper
        extends IBaseMapper<Notification, NotificationEntity> {
}
