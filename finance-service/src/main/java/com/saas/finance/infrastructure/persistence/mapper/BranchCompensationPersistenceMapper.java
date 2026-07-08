package com.saas.finance.infrastructure.persistence.mapper;

import com.saas.finance.domain.model.BranchCompensation;
import com.saas.finance.infrastructure.persistence.entity.BranchCompensationEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BranchCompensationPersistenceMapper extends IBaseMapper<BranchCompensation, BranchCompensationEntity> {
}
