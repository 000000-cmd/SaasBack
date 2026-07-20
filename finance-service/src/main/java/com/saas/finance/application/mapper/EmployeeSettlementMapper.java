package com.saas.finance.application.mapper;

import com.saas.finance.application.dto.response.EmployeeSettlementResponse;
import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface EmployeeSettlementMapper {
    EmployeeSettlementResponse toResponse(EmployeeSettlement domain);
    List<EmployeeSettlementResponse> toResponseList(List<EmployeeSettlement> domains);
}
