package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.EmployeeShiftAssignment;
import com.saas.business.infrastructure.persistence.entity.EmployeeShiftAssignmentEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeShiftAssignmentPersistenceMapper extends IBaseMapper<EmployeeShiftAssignment, EmployeeShiftAssignmentEntity> {
}
