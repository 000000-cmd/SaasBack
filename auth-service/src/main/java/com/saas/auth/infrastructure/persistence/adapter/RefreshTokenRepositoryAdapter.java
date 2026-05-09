package com.saas.auth.infrastructure.persistence.adapter;

import com.saas.auth.domain.model.RefreshToken;
import com.saas.auth.domain.port.out.IRefreshTokenRepositoryPort;
import com.saas.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.saas.auth.infrastructure.persistence.mapper.RefreshTokenPersistenceMapper;
import com.saas.auth.infrastructure.persistence.repository.JpaRefreshTokenRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryAdapter
        extends BaseJpaRepositoryAdapter<RefreshToken, RefreshTokenEntity, UUID>
        implements IRefreshTokenRepositoryPort {

    private final JpaRefreshTokenRepository jpa;

    public RefreshTokenRepositoryAdapter(JpaRefreshTokenRepository jpa,
                                         RefreshTokenPersistenceMapper mapper) {
        super(jpa, mapper, "RefreshToken");
        this.jpa = jpa;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpa.findByToken(token).map(getMapper()::toDomain);
    }

    @Override
    public List<RefreshToken> findByUserId(UUID userId) {
        return getMapper().toDomainList(jpa.findActiveByUserId(userId));
    }

    @Override
    @Transactional
    public void revokeByToken(String token) {
        jpa.revokeByToken(token, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId) {
        jpa.revokeAllByUserId(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public int deleteExpired() {
        return jpa.deleteExpired(LocalDateTime.now());
    }
}
