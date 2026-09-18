package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BusinessClient;
import com.saas.business.infrastructure.persistence.entity.BusinessClientEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessClientPersistenceMapper
        extends IBaseMapper<BusinessClient, BusinessClientEntity> {
}
