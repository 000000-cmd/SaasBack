package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.EmployeeCompensationRequest;
import com.saas.business.application.dto.response.EmployeeCompensationResponse;
import com.saas.business.domain.model.EmployeeCompensation;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeCompensationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "validFrom", ignore = true) @Mapping(target = "validTo", ignore = true)
    EmployeeCompensation toDomain(EmployeeCompensationRequest request);
    EmployeeCompensationResponse toResponse(EmployeeCompensation domain);
    List<EmployeeCompensationResponse> toResponseList(List<EmployeeCompensation> domains);
}
