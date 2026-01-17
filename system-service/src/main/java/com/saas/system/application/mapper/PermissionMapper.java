package com.saas.system.application.mapper;

import com.saas.common.mapper.IRequestMapper;
import com.saas.common.mapper.IResponseMapper;
import com.saas.system.application.dto.request.PermissionRequest;
import com.saas.system.application.dto.response.PermissionResponse;
import com.saas.system.domain.model.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para conversión de Permission entre capas.
 */
@Mapper(componentModel = "spring")
public interface PermissionMapper extends IRequestMapper<Permission, PermissionRequest>, IResponseMapper<Permission, PermissionResponse> {

    @Override
    Permission toDomain(PermissionRequest request);

    @Override
    PermissionResponse toResponse(Permission domain);
}