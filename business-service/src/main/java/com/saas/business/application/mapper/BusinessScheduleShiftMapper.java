package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BusinessScheduleShiftRequest;
import com.saas.business.application.dto.response.BusinessScheduleShiftResponse;
import com.saas.business.domain.model.BusinessScheduleShift;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.*;
import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessScheduleShiftMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    BusinessScheduleShift toDomain(BusinessScheduleShiftRequest request);
    BusinessScheduleShiftResponse toResponse(BusinessScheduleShift domain);
    List<BusinessScheduleShiftResponse> toResponseList(List<BusinessScheduleShift> domains);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "businessScheduleId", ignore = true)
    void updateDomain(BusinessScheduleShiftRequest request, @MappingTarget BusinessScheduleShift domain);
}
