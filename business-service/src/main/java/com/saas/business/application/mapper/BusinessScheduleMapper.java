package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BusinessScheduleRequest;
import com.saas.business.application.dto.response.BusinessScheduleResponse;
import com.saas.business.domain.model.BusinessSchedule;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessScheduleMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "validTo", ignore = true)
    BusinessSchedule toDomain(BusinessScheduleRequest request);
    BusinessScheduleResponse toResponse(BusinessSchedule domain);
    List<BusinessScheduleResponse> toResponseList(List<BusinessSchedule> domains);
}
