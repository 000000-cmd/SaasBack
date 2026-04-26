package com.saas.auth.domain.port.in;

import com.saas.auth.domain.model.User;
import com.saas.common.port.in.IGenericUseCase;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IUserUseCase extends IGenericUseCase<User, UUID> {

    User createWithPassword(User user, String rawPassword);

    void changePassword(UUID userId, String currentPassword, String newPassword);

    /** Reemplaza completamente el set de roles del usuario. */
    void assignRoles(UUID userId, Set<UUID> roleIds);

    Optional<User> getByUsername(String username);

    /** Carga el usuario con sus roleCodes resueltos (para login / contexto). */
    User loadWithRoles(UUID userId);
}
