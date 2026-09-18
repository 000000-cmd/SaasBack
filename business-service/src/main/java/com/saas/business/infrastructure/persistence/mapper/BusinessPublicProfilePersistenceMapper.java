package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BusinessPublicProfile;
import com.saas.business.infrastructure.persistence.entity.BusinessPublicProfileEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessPublicProfilePersistenceMapper extends IBaseMapper<BusinessPublicProfile, BusinessPublicProfileEntity> {
}
