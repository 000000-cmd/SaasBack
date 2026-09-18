package com.saas.finance.infrastructure.persistence.mapper;

import com.saas.finance.domain.model.PayrollRun;
import com.saas.finance.infrastructure.persistence.entity.PayrollRunEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface PayrollRunPersistenceMapper extends IBaseMapper<PayrollRun, PayrollRunEntity> {
}
