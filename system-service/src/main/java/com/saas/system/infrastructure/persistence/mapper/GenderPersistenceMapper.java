package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.Gender;
import com.saas.system.infrastructure.persistence.entity.GenderEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface GenderPersistenceMapper
        extends IBaseMapper<Gender, GenderEntity> {
}
