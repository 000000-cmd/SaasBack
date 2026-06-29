package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BusinessDomain;
import com.saas.common.port.in.IGenericUseCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IBusinessDomainUseCase extends IGenericUseCase<BusinessDomain, UUID> {

    /** Resuelve un dominio por su slug (subdominio). */
    Optional<BusinessDomain> findBySlug(String slug);

    /** Dominios de una empresa. */
    List<BusinessDomain> findByBusinessId(UUID businessId);
}
