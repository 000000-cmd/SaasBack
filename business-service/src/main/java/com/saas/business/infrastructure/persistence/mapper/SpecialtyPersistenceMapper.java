package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.Specialty;
import com.saas.business.infrastructure.persistence.entity.SpecialtyEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface SpecialtyPersistenceMapper extends IBaseMapper<Specialty, SpecialtyEntity> {
}
