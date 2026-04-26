package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.Constant;
import com.saas.system.infrastructure.persistence.entity.ConstantEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface ConstantPersistenceMapper extends IBaseMapper<Constant, ConstantEntity> {
}
