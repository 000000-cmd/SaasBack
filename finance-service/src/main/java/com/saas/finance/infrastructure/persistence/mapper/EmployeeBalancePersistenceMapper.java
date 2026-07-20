package com.saas.finance.infrastructure.persistence.mapper;

import com.saas.finance.domain.model.EmployeeBalance;
import com.saas.finance.infrastructure.persistence.entity.EmployeeBalanceEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeBalancePersistenceMapper extends IBaseMapper<EmployeeBalance, EmployeeBalanceEntity> {
}
