package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.OfferingRequest;
import com.saas.business.application.dto.response.OfferingResponse;
import com.saas.business.domain.model.Offering;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface OfferingMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    Offering toDomain(OfferingRequest request);
    OfferingResponse toResponse(Offering domain);
    List<OfferingResponse> toResponseList(List<Offering> domains);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    void updateDomain(OfferingRequest request, @MappingTarget Offering domain);
}
