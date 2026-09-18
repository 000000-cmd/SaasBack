package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.EmployeeOffering;
import com.saas.business.infrastructure.persistence.entity.EmployeeOfferingEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeOfferingPersistenceMapper
        extends IBaseMapper<EmployeeOffering, EmployeeOfferingEntity> {
}
