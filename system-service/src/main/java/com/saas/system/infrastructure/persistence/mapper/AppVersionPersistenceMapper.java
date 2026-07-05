package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.AppVersion;
import com.saas.system.infrastructure.persistence.entity.AppVersionEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface AppVersionPersistenceMapper extends IBaseMapper<AppVersion, AppVersionEntity> {
}
