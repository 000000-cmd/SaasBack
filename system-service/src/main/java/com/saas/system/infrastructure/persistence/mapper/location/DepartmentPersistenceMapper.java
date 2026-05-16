package com.saas.system.infrastructure.persistence.mapper.location;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.location.Department;
import com.saas.system.infrastructure.persistence.entity.location.DepartmentEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface DepartmentPersistenceMapper extends IBaseMapper<Department, DepartmentEntity> {
}
