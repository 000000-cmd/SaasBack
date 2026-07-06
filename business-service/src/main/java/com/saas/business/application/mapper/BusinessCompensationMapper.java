package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BusinessCompensationRequest;
import com.saas.business.application.dto.response.BusinessCompensationResponse;
import com.saas.business.domain.model.BusinessCompensation;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessCompensationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "validFrom", ignore = true) @Mapping(target = "validTo", ignore = true)
    BusinessCompensation toDomain(BusinessCompensationRequest request);
    BusinessCompensationResponse toResponse(BusinessCompensation domain);
    List<BusinessCompensationResponse> toResponseList(List<BusinessCompensation> domains);
}
