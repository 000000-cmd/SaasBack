package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BranchOfferingRequest;
import com.saas.business.application.dto.response.BranchOfferingResponse;
import com.saas.business.domain.model.BranchOffering;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface BranchOfferingMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    BranchOffering toDomain(BranchOfferingRequest request);
    BranchOfferingResponse toResponse(BranchOffering domain);
    List<BranchOfferingResponse> toResponseList(List<BranchOffering> domains);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "branchId", ignore = true)
    void updateDomain(BranchOfferingRequest request, @MappingTarget BranchOffering domain);
}
