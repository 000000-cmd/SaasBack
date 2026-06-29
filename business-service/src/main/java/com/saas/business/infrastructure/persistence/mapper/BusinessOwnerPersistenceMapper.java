package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BusinessOwner;
import com.saas.business.infrastructure.persistence.entity.BusinessOwnerEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessOwnerPersistenceMapper extends IBaseMapper<BusinessOwner, BusinessOwnerEntity> {
}
