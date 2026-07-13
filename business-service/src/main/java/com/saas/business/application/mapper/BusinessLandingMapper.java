package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BusinessLandingRequest;
import com.saas.business.application.dto.response.BusinessLandingResponse;
import com.saas.business.domain.model.BusinessLanding;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessLandingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    BusinessLanding toDomain(BusinessLandingRequest request);

    BusinessLandingResponse toResponse(BusinessLanding domain);
}
