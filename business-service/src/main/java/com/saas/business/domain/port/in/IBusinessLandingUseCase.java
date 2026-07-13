package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BusinessLanding;

import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso de la landing pública del negocio. Es un agregado 1:1 con la
 * empresa, por eso la operación de escritura es un upsert por businessId (no
 * hay "crear" y "actualizar" separados para el dueño).
 */
public interface IBusinessLandingUseCase {

    Optional<BusinessLanding> findByBusiness(UUID businessId);

    /** Crea o actualiza la landing del negocio (una sola por empresa). */
    BusinessLanding upsert(UUID businessId, BusinessLanding incoming);
}
