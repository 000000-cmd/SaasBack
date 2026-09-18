package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.common.port.in.IGenericUseCase;

import java.util.UUID;

public interface IBusinessBookingPolicyUseCase
        extends IGenericUseCase<BusinessBookingPolicy, UUID> {

    /**
     * La politica del negocio, o los valores por defecto si nunca la toco.
     *
     * <p>No crea la fila: una consulta no deberia escribir. Nace la primera vez
     * que alguien guarda desde la pantalla de configuracion.</p>
     */
    BusinessBookingPolicy forBusiness(UUID businessId);

    /** Crea o actualiza la fila del negocio. */
    BusinessBookingPolicy saveFor(UUID businessId, BusinessBookingPolicy incoming);
}
