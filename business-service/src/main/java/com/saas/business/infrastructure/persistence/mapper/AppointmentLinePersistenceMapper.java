package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.AppointmentLine;
import com.saas.business.infrastructure.persistence.entity.AppointmentLineEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface AppointmentLinePersistenceMapper
        extends IBaseMapper<AppointmentLine, AppointmentLineEntity> {
}
