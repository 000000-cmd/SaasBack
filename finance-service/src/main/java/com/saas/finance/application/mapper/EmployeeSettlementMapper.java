package com.saas.finance.application.mapper;

import com.saas.finance.application.dto.response.EmployeeSettlementResponse;
import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeSettlementMapper {

    /** El pendiente de acuse sale del método del dominio, no de una copia de la regla. */
    @Mapping(target = "cashPendingConfirmation", expression = "java(domain.isCashPendingConfirmation())")
    EmployeeSettlementResponse toResponse(EmployeeSettlement domain);

    List<EmployeeSettlementResponse> toResponseList(List<EmployeeSettlement> domains);
}
