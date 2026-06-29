package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BusinessRequest;
import com.saas.business.application.dto.response.BusinessResponse;
import com.saas.business.domain.model.Business;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;

import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    Business toDomain(BusinessRequest request);

    BusinessResponse toResponse(Business domain);
    List<BusinessResponse> toResponseList(List<Business> domains);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    void updateDomain(BusinessRequest request, @MappingTarget Business domain);
}
