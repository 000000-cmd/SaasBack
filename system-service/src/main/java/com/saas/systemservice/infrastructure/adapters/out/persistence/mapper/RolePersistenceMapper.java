package com.saas.systemservice.infrastructure.adapters.out.persistence.mapper;

import com.saas.saascommon.infrastructure.mapper.IBaseMapper;
import com.saas.systemservice.domain.model.lists.Role;
import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.lists.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RolePersistenceMapper extends IBaseMapper<Role, RoleEntity> {
}
