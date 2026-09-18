package com.saas.finance.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.finance.domain.model.PayrollRun;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPayrollRunRepositoryPort extends IGenericRepositoryPort<PayrollRun, UUID> {

    /** La corrida que ya se hizo con esa clave de envio, si existe. */
    Optional<PayrollRun> findByIdempotencyKey(UUID businessId, String idempotencyKey);

    List<PayrollRun> findByBusiness(UUID businessId, LocalDateTime from, LocalDateTime to, int page, int size);

    long countByBusiness(UUID businessId, LocalDateTime from, LocalDateTime to);

    boolean existsByCode(String code);
}
