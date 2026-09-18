package com.saas.finance.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.finance.domain.model.PayrollRun;
import com.saas.finance.domain.port.out.IPayrollRunRepositoryPort;
import com.saas.finance.infrastructure.persistence.entity.PayrollRunEntity;
import com.saas.finance.infrastructure.persistence.mapper.PayrollRunPersistenceMapper;
import com.saas.finance.infrastructure.persistence.repository.JpaPayrollRunRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class PayrollRunRepositoryAdapter
        extends BaseJpaRepositoryAdapter<PayrollRun, PayrollRunEntity, UUID>
        implements IPayrollRunRepositoryPort {

    private final JpaPayrollRunRepository jpa;

    public PayrollRunRepositoryAdapter(JpaPayrollRunRepository jpa, PayrollRunPersistenceMapper mapper) {
        super(jpa, mapper, "Dispersion de nomina");
        this.jpa = jpa;
    }

    @Override public java.util.Optional<PayrollRun> findByIdempotencyKey(UUID businessId, String key) {
        if (key == null || key.isBlank()) return java.util.Optional.empty();
        return jpa.findByBusinessIdAndIdempotencyKey(businessId, key).map(getMapper()::toDomain);
    }

    @Override public List<PayrollRun> findByBusiness(UUID businessId, LocalDateTime from, LocalDateTime to,
                                                     int page, int size) {
        return getMapper().toDomainList(jpa.findByBusinessIdAndExecutedAtBetweenOrderByExecutedAtDesc(
                businessId, from, to, PageRequest.of(page, size)));
    }

    @Override public long countByBusiness(UUID businessId, LocalDateTime from, LocalDateTime to) {
        return jpa.countByBusinessIdAndExecutedAtBetween(businessId, from, to);
    }

    @Override public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
