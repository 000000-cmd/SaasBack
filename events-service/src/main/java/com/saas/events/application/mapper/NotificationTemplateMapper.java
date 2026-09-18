package com.saas.events.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.events.application.dto.request.NotificationTemplateRequest;
import com.saas.events.application.dto.response.NotificationTemplateResponse;
import com.saas.events.domain.model.NotificationTemplate;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = BaseMapStructConfig.class)
public interface NotificationTemplateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    NotificationTemplate toDomain(NotificationTemplateRequest request);

    NotificationTemplateResponse toResponse(NotificationTemplate domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    void updateDomain(NotificationTemplateRequest request, @MappingTarget NotificationTemplate domain);
}
