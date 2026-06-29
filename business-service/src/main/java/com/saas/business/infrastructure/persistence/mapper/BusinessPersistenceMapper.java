package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.Business;
import com.saas.business.infrastructure.persistence.entity.BusinessEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessPersistenceMapper extends IBaseMapper<Business, BusinessEntity> {
}
