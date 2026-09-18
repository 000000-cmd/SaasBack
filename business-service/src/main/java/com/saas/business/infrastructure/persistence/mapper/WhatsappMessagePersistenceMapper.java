package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.WhatsappMessage;
import com.saas.business.infrastructure.persistence.entity.WhatsappMessageEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface WhatsappMessagePersistenceMapper extends IBaseMapper<WhatsappMessage, WhatsappMessageEntity> {
}
