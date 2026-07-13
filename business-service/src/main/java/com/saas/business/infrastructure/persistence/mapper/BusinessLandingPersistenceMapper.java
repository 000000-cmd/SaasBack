package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BusinessLanding;
import com.saas.business.infrastructure.persistence.entity.BusinessLandingEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessLandingPersistenceMapper extends IBaseMapper<BusinessLanding, BusinessLandingEntity> {
}
