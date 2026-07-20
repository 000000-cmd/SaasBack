package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.SpecialtyRequest;
import com.saas.business.application.dto.response.SpecialtyResponse;
import com.saas.business.domain.model.Specialty;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface SpecialtyMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    Specialty toDomain(SpecialtyRequest request);
    SpecialtyResponse toResponse(Specialty domain);
    List<SpecialtyResponse> toResponseList(List<Specialty> domains);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    void updateDomain(SpecialtyRequest request, @MappingTarget Specialty domain);

    @AfterMapping
    default void applyDefaults(@MappingTarget Specialty domain) {
        if (domain.getDisplayOrder() == null) domain.setDisplayOrder(0);
    }
}
