package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.Appointment;
import com.saas.business.infrastructure.persistence.entity.AppointmentEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapStructConfig.class)
public interface AppointmentPersistenceMapper extends IBaseMapper<Appointment, AppointmentEntity> {

    /** Las lineas viven en su propia tabla; las carga el adaptador aparte. */
    @Override
    @Mapping(target = "lines", ignore = true)
    Appointment toDomain(AppointmentEntity entity);
}
