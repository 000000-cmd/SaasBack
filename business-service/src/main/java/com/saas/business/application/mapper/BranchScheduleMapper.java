package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BranchScheduleRequest;
import com.saas.business.application.dto.response.BranchScheduleResponse;
import com.saas.business.domain.model.BranchSchedule;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface BranchScheduleMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "validTo", ignore = true)
    BranchSchedule toDomain(BranchScheduleRequest request);
    BranchScheduleResponse toResponse(BranchSchedule domain);
    List<BranchScheduleResponse> toResponseList(List<BranchSchedule> domains);
}
