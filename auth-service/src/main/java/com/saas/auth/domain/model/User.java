package com.saas.auth.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Modelo de dominio de Usuario.
 *
 * Notas:
 *   - {@code passwordHash} solo se mueve entre capas internas; nunca se expone via DTO.
 *   - {@code roleCodes} es transient (no se persiste en {@code app_user}). Lo poblamos
 *     al consultar {@link com.saas.auth.domain.port.out.IUserRoleRepositoryPort} +
 *     resolver Role.Code via Feign a system-service. Se incluye aqui por ergonomia
 *     (login response, JWT generation).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = {"passwordHash"})
public class User extends BaseDomain {

    private String username;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String profilePhoto;
    private String theme;
    private String languageCode;
    private LocalDateTime lastLoginAt;

    /** Solo en memoria; no se persiste en app_user. */
    @Builder.Default
    private Set<String> roleCodes = new HashSet<>();

    public String getFullName() {
        return (firstName == null ? "" : firstName) + " "
                + (lastName == null ? "" : lastName);
    }

    public boolean hasRole(String roleCode) {
        return roleCodes != null && roleCodes.contains(roleCode);
    }
}
