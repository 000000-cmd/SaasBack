package com.saas.system.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.system.application.dto.request.MenuRequest;
import com.saas.system.application.dto.response.MenuResponse;
import com.saas.system.domain.model.Menu;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = BaseMapStructConfig.class)
public interface MenuMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    Menu toDomain(MenuRequest request);

    /** children se popla externamente al construir el arbol. */
    @Mapping(target = "children", ignore = true)
    MenuResponse toResponse(Menu domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    void updateDomain(MenuRequest request, @MappingTarget Menu domain);
}
