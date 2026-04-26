package com.saas.system.domain.port.in;

import com.saas.common.port.in.ICodeUseCase;
import com.saas.system.domain.model.Role;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IRoleUseCase extends ICodeUseCase<Role, UUID> {
    List<Role> findByIds(Set<UUID> ids);
}
