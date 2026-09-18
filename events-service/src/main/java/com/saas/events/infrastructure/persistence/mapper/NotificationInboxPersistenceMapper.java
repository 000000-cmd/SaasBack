package com.saas.events.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.events.domain.model.NotificationInbox;
import com.saas.events.infrastructure.persistence.entity.NotificationInboxEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface NotificationInboxPersistenceMapper extends IBaseMapper<NotificationInbox, NotificationInboxEntity> {
}
