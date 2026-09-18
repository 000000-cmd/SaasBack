package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.business.infrastructure.persistence.entity.BusinessBookingPolicyEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessBookingPolicyPersistenceMapper
        extends IBaseMapper<BusinessBookingPolicy, BusinessBookingPolicyEntity> {
}
