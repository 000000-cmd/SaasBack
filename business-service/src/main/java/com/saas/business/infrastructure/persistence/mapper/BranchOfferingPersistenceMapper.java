package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.BranchOffering;
import com.saas.business.infrastructure.persistence.entity.BranchOfferingEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BranchOfferingPersistenceMapper extends IBaseMapper<BranchOffering, BranchOfferingEntity> {
}
