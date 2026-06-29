package com.saas.thirdparty.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.thirdparty.domain.model.ThirdPartyAddress;
import com.saas.thirdparty.infrastructure.persistence.entity.ThirdPartyAddressEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface ThirdPartyAddressPersistenceMapper extends IBaseMapper<ThirdPartyAddress, ThirdPartyAddressEntity> {
}
