package com.saas.events.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.events.domain.model.NotificationTemplate;
import com.saas.events.infrastructure.persistence.entity.NotificationTemplateEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface NotificationTemplatePersistenceMapper
        extends IBaseMapper<NotificationTemplate, NotificationTemplateEntity> {
}
