package com.saas.system.application.mapper;

import com.saas.common.mapper.IRequestMapper;
import com.saas.common.mapper.IResponseMapper;
import com.saas.system.application.dto.request.ConstantRequest;
import com.saas.system.application.dto.response.ConstantResponse;
import com.saas.system.domain.model.Constant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para conversión de Constant entre capas.
 */
@Mapper(componentModel = "spring")
public interface ConstantMapper extends IRequestMapper<Constant, ConstantRequest>, IResponseMapper<Constant, ConstantResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    Constant toDomain(ConstantRequest request);

    @Override
    ConstantResponse toResponse(Constant domain);
}
