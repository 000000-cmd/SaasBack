package com.saas.finance.application.mapper;

import com.saas.finance.application.dto.response.PayrollRunResponse;
import com.saas.finance.domain.model.PayrollRun;
import com.saas.common.mapper.BaseMapStructConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface PayrollRunMapper {
    PayrollRunResponse toResponse(PayrollRun domain);
    List<PayrollRunResponse> toResponseList(List<PayrollRun> domains);
}
