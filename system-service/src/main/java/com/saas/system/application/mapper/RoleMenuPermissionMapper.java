package com.saas.system.application.mapper;

import com.saas.common.mapper.IResponseMapper;
import com.saas.system.application.dto.response.RoleMenuPermissionResponse;
import com.saas.system.domain.model.RoleMenuPermission;
import org.mapstruct.Mapper;

/**
 * Mapper para conversión de RoleMenuPermission entre capas.
 */
@Mapper(componentModel = "spring")
public interface RoleMenuPermissionMapper extends IResponseMapper<RoleMenuPermission, RoleMenuPermissionResponse> {

    @Override
    RoleMenuPermissionResponse toResponse(RoleMenuPermission domain);
}
