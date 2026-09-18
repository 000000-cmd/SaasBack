package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BookingPolicyRequest;
import com.saas.business.application.dto.response.BookingPolicyResponse;
import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapStructConfig.class)
public interface BookingPolicyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "whatsappMonthlyCount", ignore = true)
    @Mapping(target = "whatsappCountResetAt", ignore = true)
    @Mapping(target = "enabled", ignore = true) @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true) @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true) @Mapping(target = "createdDate", ignore = true)
    BusinessBookingPolicy toDomain(BookingPolicyRequest request);

    BookingPolicyResponse toResponse(BusinessBookingPolicy domain);
}
