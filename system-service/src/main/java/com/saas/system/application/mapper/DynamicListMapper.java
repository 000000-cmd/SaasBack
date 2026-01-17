package com.saas.system.application.mapper;

import com.saas.common.mapper.IRequestMapper;
import com.saas.common.mapper.IResponseMapper;
import com.saas.system.application.dto.request.DynamicListRequest;
import com.saas.system.application.dto.response.DynamicListResponse;
import com.saas.system.domain.model.DynamicList;
import org.mapstruct.Mapper;

/**
 * Mapper para conversión de DynamicList entre capas.
 */
@Mapper(componentModel = "spring")
public interface DynamicListMapper extends
        IRequestMapper<DynamicList, DynamicListRequest>,
        IResponseMapper<DynamicList, DynamicListResponse> {

    @Override
    DynamicList toDomain(DynamicListRequest request);

    @Override
    DynamicListResponse toResponse(DynamicList domain);
}
