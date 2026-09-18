package com.saas.events.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.events.application.dto.request.NotificationRequest;
import com.saas.events.application.dto.response.NotificationResponse;
import com.saas.events.domain.model.Notification;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = BaseMapStructConfig.class)
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    Notification toDomain(NotificationRequest request);

    /** channels no sale del dominio: lo rellena el controlador tras mapear. */
    @Mapping(target = "channels", ignore = true)
    NotificationResponse toResponse(Notification domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    void updateDomain(NotificationRequest request, @MappingTarget Notification domain);
}
