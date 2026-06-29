package com.saas.thirdparty.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.thirdparty.domain.model.ThirdPartyContact;
import com.saas.thirdparty.infrastructure.persistence.entity.ThirdPartyContactEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface ThirdPartyContactPersistenceMapper extends IBaseMapper<ThirdPartyContact, ThirdPartyContactEntity> {
}
