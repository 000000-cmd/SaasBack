package com.saas.system.domain.port.out;

import com.saas.common.port.out.ICodeRepositoryPort;
import com.saas.system.domain.model.Role;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IRoleRepositoryPort extends ICodeRepositoryPort<Role, UUID> {

    /** Resuelve roles por sus Ids (usado por auth-service via Feign). */
    List<Role> findAllByIds(Set<UUID> ids);
}
