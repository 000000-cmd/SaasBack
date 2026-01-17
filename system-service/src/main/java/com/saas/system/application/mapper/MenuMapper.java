package com.saas.system.application.mapper;

import com.saas.common.mapper.IRequestMapper;
import com.saas.common.mapper.IResponseMapper;
import com.saas.system.application.dto.request.MenuRequest;
import com.saas.system.application.dto.response.MenuResponse;
import com.saas.system.domain.model.Menu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para conversión de Menu entre capas.
 */
@Mapper(componentModel = "spring")
public interface MenuMapper extends IRequestMapper<Menu, MenuRequest>, IResponseMapper<Menu, MenuResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    Menu toDomain(MenuRequest request);

    @Override
    @Mapping(target = "children", ignore = true)
    MenuResponse toResponse(Menu domain);
}