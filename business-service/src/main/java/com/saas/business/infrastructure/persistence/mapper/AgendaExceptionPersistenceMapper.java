package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.AgendaException;
import com.saas.business.infrastructure.persistence.entity.AgendaExceptionEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface AgendaExceptionPersistenceMapper
        extends IBaseMapper<AgendaException, AgendaExceptionEntity> {
}
