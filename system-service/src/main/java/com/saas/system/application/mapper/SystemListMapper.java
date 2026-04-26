package com.saas.system.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.system.application.dto.request.SystemListRequest;
import com.saas.system.application.dto.response.SystemListResponse;
import com.saas.system.domain.model.SystemList;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = BaseMapStructConfig.class)
public interface SystemListMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    SystemList toDomain(SystemListRequest request);

    SystemListResponse toResponse(SystemList domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    void updateDomain(SystemListRequest request, @MappingTarget SystemList domain);
}
