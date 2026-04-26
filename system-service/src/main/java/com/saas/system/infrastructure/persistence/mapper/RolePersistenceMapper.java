package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.Role;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface RolePersistenceMapper extends IBaseMapper<Role, RoleEntity> {
}
