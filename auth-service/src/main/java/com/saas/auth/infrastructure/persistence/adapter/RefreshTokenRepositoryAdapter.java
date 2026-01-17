package com.saas.auth.infrastructure.persistence.adapter;

import com.saas.auth.domain.model.RefreshToken;
import com.saas.auth.domain.port.out.IRefreshTokenRepositoryPort;
import com.saas.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.saas.auth.infrastructure.persistence.mapper.RefreshTokenPersistenceMapper;
import com.saas.auth.infrastructure.persistence.repository.JpaRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia para RefreshToken.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements IRefreshTokenRepositoryPort {

    private final JpaRefreshTokenRepository jpaRepository;
    private final RefreshTokenPersistenceMapper mapper;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = mapper.toEntity(refreshToken);
        RefreshTokenEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByUserId(String userId) {
        if (userId == null) return Optional.empty();
        return jpaRepository.findByUserId(UUID.fromString(userId))
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteByToken(String token) {
        jpaRepository.deleteByToken(token);
    }

    @Override
    @Transactional
    public void deleteByUserId(String userId) {
        if (userId != null) {
            jpaRepository.deleteByUserId(UUID.fromString(userId));
        }
    }

    @Override
    public boolean existsByToken(String token) {
        return jpaRepository.existsByToken(token);
    }
}
