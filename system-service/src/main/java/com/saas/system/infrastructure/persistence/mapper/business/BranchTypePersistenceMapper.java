package com.saas.system.infrastructure.persistence.mapper.business;


import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.business.BranchType;
import com.saas.system.infrastructure.persistence.entity.business.BranchTypeEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BranchTypePersistenceMapper extends IBaseMapper<BranchType, BranchTypeEntity> {

}
