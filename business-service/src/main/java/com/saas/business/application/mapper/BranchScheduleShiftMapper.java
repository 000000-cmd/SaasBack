package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BranchScheduleShiftRequest;
import com.saas.business.application.dto.response.BranchScheduleShiftResponse;
import com.saas.business.domain.model.BranchScheduleShift;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface BranchScheduleShiftMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    BranchScheduleShift toDomain(BranchScheduleShiftRequest request);
    BranchScheduleShiftResponse toResponse(BranchScheduleShift domain);
    List<BranchScheduleShiftResponse> toResponseList(List<BranchScheduleShift> domains);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "branchScheduleId", ignore = true)
    void updateDomain(BranchScheduleShiftRequest request, @MappingTarget BranchScheduleShift domain);
}
