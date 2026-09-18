package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.NotificationType;
import com.saas.system.infrastructure.persistence.entity.NotificationTypeEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface NotificationTypePersistenceMapper
        extends IBaseMapper<NotificationType, NotificationTypeEntity> {
}
