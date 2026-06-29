package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BranchSchedule;
import com.saas.business.infrastructure.persistence.entity.BranchScheduleEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BranchSchedulePersistenceMapper extends IBaseMapper<BranchSchedule, BranchScheduleEntity> {
}
