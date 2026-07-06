package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BranchCompensationRequest;
import com.saas.business.application.dto.response.BranchCompensationResponse;
import com.saas.business.domain.model.BranchCompensation;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface BranchCompensationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "validFrom", ignore = true) @Mapping(target = "validTo", ignore = true)
    BranchCompensation toDomain(BranchCompensationRequest request);
    BranchCompensationResponse toResponse(BranchCompensation domain);
    List<BranchCompensationResponse> toResponseList(List<BranchCompensation> domains);
}
