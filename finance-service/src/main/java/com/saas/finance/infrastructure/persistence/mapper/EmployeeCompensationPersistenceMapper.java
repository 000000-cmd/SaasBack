package com.saas.finance.infrastructure.persistence.mapper;

import com.saas.finance.domain.model.EmployeeCompensation;
import com.saas.finance.infrastructure.persistence.entity.EmployeeCompensationEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeCompensationPersistenceMapper extends IBaseMapper<EmployeeCompensation, EmployeeCompensationEntity> {
}
