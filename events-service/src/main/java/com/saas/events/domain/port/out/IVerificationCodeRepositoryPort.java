package com.saas.events.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.events.domain.model.VerificationCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IVerificationCodeRepositoryPort extends IGenericRepositoryPort<VerificationCode, UUID> {
    /** El codigo vivo de un destino: el mas reciente sin consumir. */
    Optional<VerificationCode> findActive(String target, String purpose);
    /** Todos los vivos: se invalidan en bloque al pedir uno nuevo. */
    List<VerificationCode> findAllActive(String target, String purpose);
    long deleteExpiredBefore(LocalDateTime cutoff);
}
