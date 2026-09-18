package com.saas.finance.application.mapper;

import com.saas.finance.application.dto.response.ServiceChargeResponse;
import com.saas.finance.domain.model.ServiceCharge;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface ServiceChargeMapper {

    /** `missingReceipt` sale del metodo del dominio, no de una copia de la regla. */
    @Mapping(target = "missingReceipt", expression = "java(domain.isMissingReceipt())")
    ServiceChargeResponse toResponse(ServiceCharge domain);

    List<ServiceChargeResponse> toResponseList(List<ServiceCharge> domains);
}
