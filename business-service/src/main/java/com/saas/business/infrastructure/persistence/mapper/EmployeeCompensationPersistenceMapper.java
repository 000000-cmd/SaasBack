package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.EmployeeCompensation;
import com.saas.business.infrastructure.persistence.entity.EmployeeCompensationEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeCompensationPersistenceMapper extends IBaseMapper<EmployeeCompensation, EmployeeCompensationEntity> {
}
