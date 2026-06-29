package com.saas.thirdparty.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.thirdparty.application.dto.request.ThirdPartyRequest;
import com.saas.thirdparty.application.dto.response.ThirdPartyResponse;
import com.saas.thirdparty.domain.model.ThirdParty;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.stream.Stream;

@Mapper(config = BaseMapStructConfig.class)
public interface ThirdPartyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    ThirdParty toDomain(ThirdPartyRequest request);

    @Mapping(target = "fullName", expression = "java(buildFullName(domain))")
    ThirdPartyResponse toResponse(ThirdParty domain);

    List<ThirdPartyResponse> toResponseList(List<ThirdParty> domains);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    void updateDomain(ThirdPartyRequest request, @MappingTarget ThirdParty domain);

    default String buildFullName(ThirdParty t) {
        if (t == null) return null;
        return String.join(" ",
                Stream.of(t.getFirstName(), t.getSecondName(), t.getFirstLastName(), t.getSecondLastName())
                        .filter(s -> s != null && !s.isBlank())
                        .toList());
    }
}
