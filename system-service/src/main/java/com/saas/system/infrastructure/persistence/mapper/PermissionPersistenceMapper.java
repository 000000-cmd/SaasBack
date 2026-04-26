package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.Permission;
import com.saas.system.infrastructure.persistence.entity.PermissionEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface PermissionPersistenceMapper extends IBaseMapper<Permission, PermissionEntity> {
}
