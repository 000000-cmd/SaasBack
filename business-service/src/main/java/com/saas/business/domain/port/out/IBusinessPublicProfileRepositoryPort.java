package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BusinessPublicProfile;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.Optional;
import java.util.UUID;

public interface IBusinessPublicProfileRepositoryPort
        extends IGenericRepositoryPort<BusinessPublicProfile, UUID> {

    Optional<BusinessPublicProfile> findByBusinessId(UUID businessId);

    /**
     * Suma una resena al agregado, en la base y de una vez.
     *
     * @return false si ese negocio no tiene ficha publica todavia.
     */
    boolean addReview(UUID businessId, int estrellas);
}
