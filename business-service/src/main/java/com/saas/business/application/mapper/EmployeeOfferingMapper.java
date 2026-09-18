package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.EmployeeOfferingRequest;
import com.saas.business.application.dto.response.EmployeeOfferingResponse;
import com.saas.business.domain.model.EmployeeOffering;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeOfferingMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    EmployeeOffering toDomain(EmployeeOfferingRequest request);

    EmployeeOfferingResponse toResponse(EmployeeOffering domain);

    List<EmployeeOfferingResponse> toResponseList(List<EmployeeOffering> domains);

    List<EmployeeOffering> toDomainList(List<EmployeeOfferingRequest> requests);
}
