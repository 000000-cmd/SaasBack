package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BranchCompensation;
import com.saas.business.infrastructure.persistence.entity.BranchCompensationEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BranchCompensationPersistenceMapper extends IBaseMapper<BranchCompensation, BranchCompensationEntity> {
}
