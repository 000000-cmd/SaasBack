package com.saas.finance.infrastructure.persistence.mapper;

import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.finance.infrastructure.persistence.entity.EmployeeSettlementEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeSettlementPersistenceMapper extends IBaseMapper<EmployeeSettlement, EmployeeSettlementEntity> {
}
