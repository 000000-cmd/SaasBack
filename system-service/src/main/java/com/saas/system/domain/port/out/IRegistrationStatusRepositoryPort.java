package com.saas.system.domain.port.out;

import com.saas.common.port.out.ICatalogRepositoryPort;
import com.saas.system.domain.model.RegistrationStatus;

import java.util.UUID;

public interface IRegistrationStatusRepositoryPort
        extends ICatalogRepositoryPort<RegistrationStatus, UUID> {
}
