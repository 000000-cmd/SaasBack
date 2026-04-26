package com.saas.auth.application.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.auth.application.dto.request.CreateUserRequest;
import com.saas.auth.application.dto.request.UpdateUserRequest;
import com.saas.auth.application.dto.response.UserResponse;
import com.saas.auth.domain.model.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = BaseMapStructConfig.class)
public interface UserMapper {

    /** El password viene en claro; AuthService/UserService lo cifra con BCrypt. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "roleCodes", ignore = true)
    User toDomain(CreateUserRequest request);

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    UserResponse toResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "auditUser", ignore = true)
    @Mapping(target = "auditDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "roleCodes", ignore = true)
    void updateDomainFromRequest(UpdateUserRequest request, @MappingTarget User domain);
}
