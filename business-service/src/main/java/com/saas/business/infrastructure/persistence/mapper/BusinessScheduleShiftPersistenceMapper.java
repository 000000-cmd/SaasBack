package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BusinessScheduleShift;
import com.saas.business.infrastructure.persistence.entity.BusinessScheduleShiftEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessScheduleShiftPersistenceMapper extends IBaseMapper<BusinessScheduleShift, BusinessScheduleShiftEntity> {
}
