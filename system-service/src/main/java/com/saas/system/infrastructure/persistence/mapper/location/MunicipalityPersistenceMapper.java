package com.saas.system.infrastructure.persistence.mapper.location;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.location.Municipality;
import com.saas.system.infrastructure.persistence.entity.location.MunicipalityEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface MunicipalityPersistenceMapper extends IBaseMapper<Municipality, MunicipalityEntity> {
}
