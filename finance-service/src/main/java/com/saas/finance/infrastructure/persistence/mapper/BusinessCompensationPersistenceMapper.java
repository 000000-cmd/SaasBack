package com.saas.finance.infrastructure.persistence.mapper;

import com.saas.finance.domain.model.BusinessCompensation;
import com.saas.finance.infrastructure.persistence.entity.BusinessCompensationEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessCompensationPersistenceMapper extends IBaseMapper<BusinessCompensation, BusinessCompensationEntity> {
}
