package com.saas.system.application.mapper;

import com.saas.common.mapper.IResponseMapper;
import com.saas.system.application.dto.response.RoleMenuResponse;
import com.saas.system.domain.model.RoleMenu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para conversión de RoleMenu entre capas.
 */
@Mapper(componentModel = "spring")
public interface RoleMenuMapper extends IResponseMapper<RoleMenu, RoleMenuResponse> {

    @Override
    @Mapping(target = "permissions", ignore = true)
    RoleMenuResponse toResponse(RoleMenu domain);
}