package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.OfferingCategoryRequest;
import com.saas.business.application.dto.response.OfferingCategoryResponse;
import com.saas.business.domain.model.OfferingCategory;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface OfferingCategoryMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    OfferingCategory toDomain(OfferingCategoryRequest request);
    OfferingCategoryResponse toResponse(OfferingCategory domain);
    List<OfferingCategoryResponse> toResponseList(List<OfferingCategory> domains);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    void updateDomain(OfferingCategoryRequest request, @MappingTarget OfferingCategory domain);

    @AfterMapping
    default void applyDefaults(@MappingTarget OfferingCategory domain) {
        if (domain.getDisplayOrder() == null) domain.setDisplayOrder(0);
    }
}
