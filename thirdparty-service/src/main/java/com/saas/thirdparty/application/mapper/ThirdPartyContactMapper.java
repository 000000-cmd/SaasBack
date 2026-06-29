package com.saas.thirdparty.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.thirdparty.application.dto.request.ThirdPartyContactRequest;
import com.saas.thirdparty.application.dto.response.ThirdPartyContactResponse;
import com.saas.thirdparty.domain.model.ThirdPartyContact;
import org.mapstruct.*;

import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface ThirdPartyContactMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "verifiedAt", ignore = true)
    ThirdPartyContact toDomain(ThirdPartyContactRequest request);

    ThirdPartyContactResponse toResponse(ThirdPartyContact domain);
    List<ThirdPartyContactResponse> toResponseList(List<ThirdPartyContact> domains);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "verifiedAt", ignore = true)
    @Mapping(target = "thirdPartyId", ignore = true)
    void updateDomain(ThirdPartyContactRequest request, @MappingTarget ThirdPartyContact domain);
}
