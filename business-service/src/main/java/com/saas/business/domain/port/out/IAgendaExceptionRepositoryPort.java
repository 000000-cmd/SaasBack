package com.saas.business.domain.port.out;

import com.saas.business.domain.model.AgendaException;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IAgendaExceptionRepositoryPort extends IGenericRepositoryPort<AgendaException, UUID> {
    /** Lo que resta agenda en el negocio dentro del rango. Alimenta el motor. */
    List<AgendaException> overlapping(UUID businessId, Instant start, Instant end);
}
