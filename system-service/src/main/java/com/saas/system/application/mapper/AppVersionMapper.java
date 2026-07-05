package com.saas.system.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.system.application.dto.response.AppVersionResponse;
import com.saas.system.domain.model.AppVersion;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = BaseMapStructConfig.class)
public interface AppVersionMapper {
    AppVersionResponse toResponse(AppVersion domain);
    List<AppVersionResponse> toResponseList(List<AppVersion> domains);
}
