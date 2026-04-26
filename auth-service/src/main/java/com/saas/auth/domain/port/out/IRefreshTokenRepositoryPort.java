package com.saas.auth.domain.port.out;

import com.saas.auth.domain.model.RefreshToken;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRefreshTokenRepositoryPort extends IGenericRepositoryPort<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserId(UUID userId);

    /** Marca como revocado (RevokedAt = now) sin borrar. */
    void revokeByToken(String token);

    /** Revoca todos los tokens activos del usuario (logout global). */
    void revokeAllByUserId(UUID userId);

    /** Hard-delete de tokens expirados; util para job de limpieza. */
    int deleteExpired();
}
