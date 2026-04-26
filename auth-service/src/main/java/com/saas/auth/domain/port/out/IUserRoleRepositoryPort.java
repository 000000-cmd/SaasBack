package com.saas.auth.domain.port.out;

import com.saas.auth.domain.model.UserRole;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IUserRoleRepositoryPort extends IGenericRepositoryPort<UserRole, UUID> {

    List<UserRole> findByUserId(UUID userId);

    /** Reemplaza completamente el set de roles de un usuario. */
    void replaceRolesForUser(UUID userId, Set<UUID> roleIds);

    /** Elimina (hard) todas las asignaciones de un usuario. */
    void deleteByUserId(UUID userId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
}
