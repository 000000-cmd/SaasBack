package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BranchScheduleShift;
import com.saas.business.infrastructure.persistence.entity.BranchScheduleShiftEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BranchScheduleShiftPersistenceMapper extends IBaseMapper<BranchScheduleShift, BranchScheduleShiftEntity> {
}
