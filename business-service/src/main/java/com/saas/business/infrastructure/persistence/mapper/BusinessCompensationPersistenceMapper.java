package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BusinessCompensation;
import com.saas.business.infrastructure.persistence.entity.BusinessCompensationEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessCompensationPersistenceMapper extends IBaseMapper<BusinessCompensation, BusinessCompensationEntity> {
}
