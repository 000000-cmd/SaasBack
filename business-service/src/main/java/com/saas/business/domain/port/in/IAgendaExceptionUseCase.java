package com.saas.business.domain.port.in;

import com.saas.business.domain.model.AgendaException;
import com.saas.common.port.in.IGenericUseCase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IAgendaExceptionUseCase extends IGenericUseCase<AgendaException, UUID> {
    List<AgendaException> inRange(UUID businessId, Instant start, Instant end);
}
