package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.EmployeeRequest;
import com.saas.business.application.dto.response.EmployeeResponse;
import com.saas.business.domain.model.Employee;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    Employee toDomain(EmployeeRequest request);

    EmployeeResponse toResponse(Employee domain);
    List<EmployeeResponse> toResponseList(List<Employee> domains);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "thirdPartyId", ignore = true)
    void updateDomain(EmployeeRequest request, @MappingTarget Employee domain);
}
