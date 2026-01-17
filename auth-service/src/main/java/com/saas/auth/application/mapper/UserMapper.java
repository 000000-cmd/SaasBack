package com.saas.auth.application.mapper;

import com.saas.auth.application.dto.request.CreateUserRequest;
import com.saas.auth.application.dto.request.UpdateUserRequest;
import com.saas.auth.application.dto.response.LoginResponse;
import com.saas.auth.application.dto.response.UserResponse;
import com.saas.auth.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mapper para conversiones de User entre capas.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    // ===== Request to Domain =====
    User toDomain(CreateUserRequest request);

    User toDomain(UpdateUserRequest request);

    // ===== Domain to Response =====

    @Mapping(target = "roles", source = "roleCodes", qualifiedByName = "toRolesList")
    UserResponse toResponse(User domain);

    List<UserResponse> toResponseList(List<User> domains);

    LoginResponse toLoginResponse(User domain);

    // ===== Helper methods =====

    @Named("toRoleCodesSet")
    default Set<String> toRoleCodesSet(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(roleCodes);
    }

    @Named("toRolesList")
    default List<String> toRolesList(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(roleCodes);
    }
}