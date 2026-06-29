package com.saas.thirdparty.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.thirdparty.application.dto.request.ThirdPartyAddressRequest;
import com.saas.thirdparty.application.dto.response.ThirdPartyAddressResponse;
import com.saas.thirdparty.domain.model.ThirdPartyAddress;
import org.mapstruct.*;

import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface ThirdPartyAddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    ThirdPartyAddress toDomain(ThirdPartyAddressRequest request);

    ThirdPartyAddressResponse toResponse(ThirdPartyAddress domain);
    List<ThirdPartyAddressResponse> toResponseList(List<ThirdPartyAddress> domains);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "thirdPartyId", ignore = true)
    void updateDomain(ThirdPartyAddressRequest request, @MappingTarget ThirdPartyAddress domain);
}
