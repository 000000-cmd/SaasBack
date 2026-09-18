package com.saas.business.application.mapper;

import com.saas.business.application.dto.request.BusinessClientRequest;
import com.saas.business.application.dto.response.BusinessClientResponse;
import com.saas.business.domain.model.BusinessClient;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.util.PhoneNumbers;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface BusinessClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    // Estos NO llegan del formulario: los mueve el flujo que los provoca (una
    // cita completada, un consentimiento aceptado). Editables desde la ficha,
    // permitirian borrar a mano una inasistencia o un opt-in.
    @Mapping(target = "phoneVerifiedAt", ignore = true)
    @Mapping(target = "whatsappOptInAt", ignore = true)
    @Mapping(target = "whatsappOptInText", ignore = true)
    @Mapping(target = "whatsappOptOutAt", ignore = true)
    @Mapping(target = "noShowCount", ignore = true)
    @Mapping(target = "visitCount", ignore = true)
    @Mapping(target = "lastVisitAt", ignore = true)
    @Mapping(target = "phoneE164", source = "phone")
    BusinessClient toDomain(BusinessClientRequest request);

    @Mapping(target = "phone", source = "phoneE164")
    @Mapping(target = "phoneLocal", expression = "java(com.saas.common.util.PhoneNumbers.toLocalDisplay(domain.getPhoneE164()))")
    @Mapping(target = "phoneVerified", expression = "java(domain.isPhoneVerified())")
    @Mapping(target = "walkIn", expression = "java(domain.isWalkIn())")
    @Mapping(target = "whatsappAllowed", expression = "java(domain.canReceiveWhatsapp())")
    BusinessClientResponse toResponse(BusinessClient domain);

    List<BusinessClientResponse> toResponseList(List<BusinessClient> domains);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "phoneVerifiedAt", ignore = true)
    @Mapping(target = "whatsappOptInAt", ignore = true)
    @Mapping(target = "whatsappOptInText", ignore = true)
    @Mapping(target = "whatsappOptOutAt", ignore = true)
    @Mapping(target = "noShowCount", ignore = true)
    @Mapping(target = "visitCount", ignore = true)
    @Mapping(target = "lastVisitAt", ignore = true)
    @Mapping(target = "phoneE164", source = "phone")
    void updateDomain(BusinessClientRequest request, @MappingTarget BusinessClient domain);
}
