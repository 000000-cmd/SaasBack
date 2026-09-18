package com.saas.system.infrastructure.persistence.mapper.flow;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.flow.FlowMessage;
import com.saas.system.infrastructure.persistence.entity.flow.FlowMessageEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface FlowMessagePersistenceMapper extends IBaseMapper<FlowMessage, FlowMessageEntity> {
}
