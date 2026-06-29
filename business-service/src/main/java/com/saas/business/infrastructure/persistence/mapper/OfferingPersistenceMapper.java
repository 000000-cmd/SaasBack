package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.Offering;
import com.saas.business.infrastructure.persistence.entity.OfferingEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface OfferingPersistenceMapper extends IBaseMapper<Offering, OfferingEntity> {
}
