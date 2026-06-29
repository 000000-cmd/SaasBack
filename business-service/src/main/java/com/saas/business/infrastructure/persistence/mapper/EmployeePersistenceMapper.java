package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.Employee;
import com.saas.business.infrastructure.persistence.entity.EmployeeEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeePersistenceMapper extends IBaseMapper<Employee, EmployeeEntity> {
}
