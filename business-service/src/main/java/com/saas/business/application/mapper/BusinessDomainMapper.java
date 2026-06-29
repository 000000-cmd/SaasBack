package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BusinessDomainRequest;
import com.saas.business.application.dto.response.BusinessDomainResponse;
import com.saas.business.domain.model.BusinessDomain;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;

import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessDomainMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "verifiedDate", ignore = true)
    BusinessDomain toDomain(BusinessDomainRequest request);

    BusinessDomainResponse toResponse(BusinessDomain domain);
    List<BusinessDomainResponse> toResponseList(List<BusinessDomain> domains);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "verifiedDate", ignore = true)
    void updateDomain(BusinessDomainRequest request, @MappingTarget BusinessDomain domain);
}
