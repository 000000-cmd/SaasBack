package com.saas.system.infrastructure.persistence.mapper.flow;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.flow.Flow;
import com.saas.system.infrastructure.persistence.entity.flow.FlowEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface FlowPersistenceMapper extends IBaseMapper<Flow, FlowEntity> {
}
