package com.saas.systemservice.application.mappers.menu;

import com.saas.saascommon.infrastructure.mapper.IRequestMapper;
import com.saas.systemservice.application.dto.request.menu.CreateMenuRequest;
import com.saas.systemservice.domain.model.menu.Menu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MenuApplicationMapper extends IRequestMapper<Menu, CreateMenuRequest> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    Menu toDomain(CreateMenuRequest request);
}
