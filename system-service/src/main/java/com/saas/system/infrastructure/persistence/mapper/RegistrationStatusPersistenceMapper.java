package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.RegistrationStatus;
import com.saas.system.infrastructure.persistence.entity.RegistrationStatusEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface RegistrationStatusPersistenceMapper
        extends IBaseMapper<RegistrationStatus, RegistrationStatusEntity> {
}
