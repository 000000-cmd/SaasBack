package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.EmployeeShiftAssignmentRequest;
import com.saas.business.application.dto.response.EmployeeShiftAssignmentResponse;
import com.saas.business.domain.model.EmployeeShiftAssignment;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeShiftAssignmentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "validFrom", ignore = true) @Mapping(target = "validTo", ignore = true)
    EmployeeShiftAssignment toDomain(EmployeeShiftAssignmentRequest request);
    EmployeeShiftAssignmentResponse toResponse(EmployeeShiftAssignment domain);
    List<EmployeeShiftAssignmentResponse> toResponseList(List<EmployeeShiftAssignment> domains);
}
