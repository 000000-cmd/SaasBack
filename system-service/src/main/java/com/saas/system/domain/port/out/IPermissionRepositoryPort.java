package com.saas.system.domain.port.out;

import com.saas.common.port.out.ICodeRepositoryPort;
import com.saas.system.domain.model.Permission;

import java.util.UUID;

public interface IPermissionRepositoryPort extends ICodeRepositoryPort<Permission, UUID> {
}
