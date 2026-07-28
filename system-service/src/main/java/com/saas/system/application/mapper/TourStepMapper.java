package com.saas.system.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.system.application.dto.request.TourStepRequest;
import com.saas.system.application.dto.response.TourStepResponse;
import com.saas.system.domain.model.TourStep;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = BaseMapStructConfig.class)
public interface TourStepMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "menuCode", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    TourStep toDomain(TourStepRequest request);

    TourStepResponse toResponse(TourStep domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "menuCode", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    void updateDomain(TourStepRequest request, @MappingTarget TourStep domain);
}
