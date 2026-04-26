package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.SystemList;
import com.saas.system.infrastructure.persistence.entity.SystemListEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface SystemListPersistenceMapper extends IBaseMapper<SystemList, SystemListEntity> {
}
