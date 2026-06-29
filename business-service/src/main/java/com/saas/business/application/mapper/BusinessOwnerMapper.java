package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BusinessOwnerRequest;
import com.saas.business.application.dto.response.BusinessOwnerResponse;
import com.saas.business.domain.model.BusinessOwner;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessOwnerMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    BusinessOwner toDomain(BusinessOwnerRequest request);

    BusinessOwnerResponse toResponse(BusinessOwner domain);
    List<BusinessOwnerResponse> toResponseList(List<BusinessOwner> domains);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "thirdPartyId", ignore = true)
    void updateDomain(BusinessOwnerRequest request, @MappingTarget BusinessOwner domain);
}
