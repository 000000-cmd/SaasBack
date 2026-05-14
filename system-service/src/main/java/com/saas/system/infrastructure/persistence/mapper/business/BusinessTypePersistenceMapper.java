package com.saas.system.infrastructure.persistence.mapper.business;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.business.BusinessType;
import com.saas.system.infrastructure.persistence.entity.business.BusinessTypeEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessTypePersistenceMapper extends IBaseMapper<BusinessType, BusinessTypeEntity> {
}
