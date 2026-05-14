package com.saas.system.domain.port.out;

import com.saas.common.port.out.ICatalogRepositoryPort;
import com.saas.system.domain.model.Gender;

import java.util.UUID;

public interface IGenderRepositoryPort
        extends ICatalogRepositoryPort<Gender, UUID> {
}
