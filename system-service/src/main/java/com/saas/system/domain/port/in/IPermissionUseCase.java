package com.saas.system.domain.port.in;

import com.saas.common.port.in.ICodeUseCase;
import com.saas.system.domain.model.Permission;

import java.util.UUID;

public interface IPermissionUseCase extends ICodeUseCase<Permission, UUID> {
}
