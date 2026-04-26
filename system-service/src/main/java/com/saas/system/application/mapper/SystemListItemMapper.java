package com.saas.system.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.system.application.dto.request.SystemListItemRequest;
import com.saas.system.application.dto.response.SystemListItemResponse;
import com.saas.system.domain.model.SystemListItem;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = BaseMapStructConfig.class)
public interface SystemListItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listId", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    SystemListItem toDomain(SystemListItemRequest request);

    SystemListItemResponse toResponse(SystemListItem domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listId", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    void updateDomain(SystemListItemRequest request, @MappingTarget SystemListItem domain);
}
