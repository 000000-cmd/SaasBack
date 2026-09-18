package com.saas.system.infrastructure.persistence.mapper.flow;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.flow.FlowControl;
import com.saas.system.infrastructure.persistence.entity.flow.FlowControlEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface FlowControlPersistenceMapper extends IBaseMapper<FlowControl, FlowControlEntity> {
}
