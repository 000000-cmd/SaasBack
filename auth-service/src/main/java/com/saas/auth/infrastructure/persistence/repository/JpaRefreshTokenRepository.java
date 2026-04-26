package com.saas.auth.infrastructure.persistence.repository;

import com.saas.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByToken(String token);

    @Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    List<RefreshTokenEntity> findActiveByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.revokedAt = :now WHERE rt.token = :token AND rt.revokedAt IS NULL")
    int revokeByToken(@Param("token") String token, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.revokedAt = :now WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
