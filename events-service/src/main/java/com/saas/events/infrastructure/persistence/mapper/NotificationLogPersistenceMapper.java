package com.saas.events.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.events.domain.model.NotificationLog;
import com.saas.events.infrastructure.persistence.entity.NotificationLogEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface NotificationLogPersistenceMapper extends IBaseMapper<NotificationLog, NotificationLogEntity> {
}
