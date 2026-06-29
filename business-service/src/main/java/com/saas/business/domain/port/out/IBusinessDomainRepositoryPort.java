package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BusinessDomain;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IBusinessDomainRepositoryPort extends IGenericRepositoryPort<BusinessDomain, UUID> {
    Optional<BusinessDomain> findBySlug(String slug);
    List<BusinessDomain> findByBusinessId(UUID businessId);
}
