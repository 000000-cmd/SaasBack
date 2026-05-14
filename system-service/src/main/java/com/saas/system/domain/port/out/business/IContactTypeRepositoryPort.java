package com.saas.system.domain.port.out.business;

import com.saas.common.port.out.ICatalogRepositoryPort;
import com.saas.system.domain.model.business.ContactType;

import java.util.UUID;

public interface IContactTypeRepositoryPort extends ICatalogRepositoryPort<ContactType, UUID> {
}
