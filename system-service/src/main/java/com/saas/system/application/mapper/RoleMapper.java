package com.saas.system.application.mapper;

import com.saas.common.mapper.IRequestMapper;
import com.saas.common.mapper.IResponseMapper;
import com.saas.system.application.dto.request.RoleRequest;
import com.saas.system.application.dto.response.RoleResponse;
import com.saas.system.domain.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para conversión de Role entre capas.
 */
@Mapper(componentModel = "spring")
public interface RoleMapper extends IRequestMapper<Role, RoleRequest>, IResponseMapper<Role, RoleResponse> {

    @Override
    Role toDomain(RoleRequest request);

    @Override
    RoleResponse toResponse(Role domain);
}