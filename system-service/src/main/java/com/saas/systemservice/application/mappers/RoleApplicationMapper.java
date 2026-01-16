package com.saas.systemservice.application.mappers;

import com.saas.saascommon.infrastructure.mapper.IRequestMapper;
import com.saas.systemservice.application.dto.request.CreateRoleRequest;
import com.saas.systemservice.domain.model.lists.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleApplicationMapper extends IRequestMapper<Role, CreateRoleRequest> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    Role toDomain(CreateRoleRequest request);
}
