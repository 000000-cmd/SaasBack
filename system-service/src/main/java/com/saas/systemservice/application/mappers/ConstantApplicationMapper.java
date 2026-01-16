package com.saas.systemservice.application.mappers;

import com.saas.saascommon.infrastructure.mapper.IRequestMapper;
import com.saas.systemservice.application.dto.request.CreateConstantRequest;
import com.saas.systemservice.domain.model.Constant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConstantApplicationMapper extends IRequestMapper<Constant, CreateConstantRequest> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    Constant toDomain(CreateConstantRequest request);
}
