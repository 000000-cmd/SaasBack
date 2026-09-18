package com.saas.events.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.events.domain.model.VerificationCode;
import com.saas.events.domain.port.out.IVerificationCodeRepositoryPort;
import com.saas.events.infrastructure.persistence.entity.VerificationCodeEntity;
import com.saas.events.infrastructure.persistence.mapper.VerificationCodePersistenceMapper;
import com.saas.events.infrastructure.persistence.repository.JpaVerificationCodeRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VerificationCodeRepositoryAdapter
        extends BaseJpaRepositoryAdapter<VerificationCode, VerificationCodeEntity, UUID>
        implements IVerificationCodeRepositoryPort {

    private final JpaVerificationCodeRepository jpa;

    public VerificationCodeRepositoryAdapter(JpaVerificationCodeRepository jpa,
                                             VerificationCodePersistenceMapper mapper) {
        super(jpa, mapper, "VerificationCode");
        this.jpa = jpa;
    }

    @Override
    public Optional<VerificationCode> findActive(String target, String purpose) {
        return jpa.findFirstByTargetAndPurposeAndConsumedAtIsNullOrderByCreatedDateDesc(target, purpose)
                .map(getMapper()::toDomain);
    }

    @Override
    public List<VerificationCode> findAllActive(String target, String purpose) {
        return jpa.findByTargetAndPurposeAndConsumedAtIsNull(target, purpose)
                .stream().map(getMapper()::toDomain).toList();
    }

    @Override
    @Transactional
    public long deleteExpiredBefore(LocalDateTime cutoff) {
        return jpa.deleteByExpiresAtBefore(cutoff);
    }
}
