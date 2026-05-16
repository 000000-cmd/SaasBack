package com.saas.system.domain.port.out.location;

import com.saas.common.port.out.ICodeRepositoryPort;
import com.saas.system.domain.model.location.Country;

import java.util.UUID;

public interface ICountryRepositoryPort extends ICodeRepositoryPort<Country, UUID> {
}
