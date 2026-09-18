package com.saas.finance.infrastructure.persistence.repository;

import com.saas.finance.infrastructure.persistence.entity.PayrollRunEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaPayrollRunRepository extends JpaRepository<PayrollRunEntity, UUID> {

    /** La corrida que ya se hizo con esa clave, si es que se hizo. */
    Optional<PayrollRunEntity> findByBusinessIdAndIdempotencyKey(UUID businessId, String idempotencyKey);

    List<PayrollRunEntity> findByBusinessIdAndExecutedAtBetweenOrderByExecutedAtDesc(
            UUID businessId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    long countByBusinessIdAndExecutedAtBetween(UUID businessId, LocalDateTime from, LocalDateTime to);

    boolean existsByCode(String code);
}
