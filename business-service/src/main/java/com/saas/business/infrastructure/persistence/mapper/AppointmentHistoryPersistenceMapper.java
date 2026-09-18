package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.AppointmentHistoryEntry;
import com.saas.business.infrastructure.persistence.entity.AppointmentHistoryEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface AppointmentHistoryPersistenceMapper
        extends IBaseMapper<AppointmentHistoryEntry, AppointmentHistoryEntity> {
}
