package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.WhatsappSession;
import com.saas.business.infrastructure.persistence.entity.WhatsappSessionEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface WhatsappSessionPersistenceMapper extends IBaseMapper<WhatsappSession, WhatsappSessionEntity> {
}
