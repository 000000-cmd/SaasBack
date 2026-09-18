package com.saas.events.infrastructure.persistence.repository;

import com.saas.events.infrastructure.persistence.entity.VerificationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaVerificationCodeRepository extends JpaRepository<VerificationCodeEntity, UUID> {

    /** El codigo vivo de un destino: el mas reciente sin consumir. */
    Optional<VerificationCodeEntity> findFirstByTargetAndPurposeAndConsumedAtIsNullOrderByCreatedDateDesc(
            String target, String purpose);

    /** Todos los vivos de un destino: se invalidan al pedir uno nuevo. */
    List<VerificationCodeEntity> findByTargetAndPurposeAndConsumedAtIsNull(String target, String purpose);

    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
