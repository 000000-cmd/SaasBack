package com.saas.finance.infrastructure.persistence.mapper;

import com.saas.finance.domain.model.ServiceCharge;
import com.saas.finance.infrastructure.persistence.entity.ServiceChargeEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface ServiceChargePersistenceMapper extends IBaseMapper<ServiceCharge, ServiceChargeEntity> {
}
