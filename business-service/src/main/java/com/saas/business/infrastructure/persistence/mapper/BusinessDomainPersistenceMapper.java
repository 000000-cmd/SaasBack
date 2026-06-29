package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BusinessDomain;
import com.saas.business.infrastructure.persistence.entity.BusinessDomainEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessDomainPersistenceMapper extends IBaseMapper<BusinessDomain, BusinessDomainEntity> {
}
