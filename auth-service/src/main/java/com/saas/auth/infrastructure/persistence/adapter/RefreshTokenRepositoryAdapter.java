package com.saas.auth.infrastructure.persistence.adapter;

import com.saas.auth.domain.model.RefreshToken;
import com.saas.auth.domain.port.out.IRefreshTokenRepositoryPort;
import com.saas.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.saas.auth.infrastructure.persistence.mapper.RefreshTokenPersistenceMapper;
import com.saas.auth.infrastructure.persistence.repository.JpaRefreshTokenRepository;
import com.saas.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements IRefreshTokenRepositoryPort {

    private final JpaRefreshTokenRepository jpa;
    private final RefreshTokenPersistenceMapper mapper;

    @Override
    public RefreshToken save(RefreshToken token) {
        return mapper.toDomain(jpa.save(mapper.toEntity(token)));
    }

    @Override
    @Transactional
    public RefreshToken update(RefreshToken token) {
        RefreshTokenEntity existing = jpa.findById(token.getId())
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", "Id", token.getId()));
        mapper.updateEntityFromDomain(token, existing);
        return mapper.toDomain(jpa.save(existing));
    }

    @Override
    public Optional<RefreshToken> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public List<RefreshToken> findAll() {
        return mapper.toDomainList(jpa.findAll());
    }

    @Override
    @Transactional
    public void softDeleteById(UUID id) {
        jpa.findById(id).ifPresent(e -> {
            e.setEnabled(false);
            e.setVisible(false);
            jpa.save(e);
        });
    }

    @Override
    public void hardDeleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpa.findByToken(token).map(mapper::toDomain);
    }

    @Override
    public List<RefreshToken> findByUserId(UUID userId) {
        return mapper.toDomainList(jpa.findActiveByUserId(userId));
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
