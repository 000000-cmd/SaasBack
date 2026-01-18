package com.saas.auth.domain.port.out;

import com.saas.auth.domain.model.RefreshToken;

import java.util.Optional;

/**
 * Puerto de salida para persistencia de refresh tokens.
 */
public interface IRefreshTokenRepositoryPort {

    /**
     * Guarda un refresh token
     */
    RefreshToken save(RefreshToken refreshToken);

    /**
     * Busca un refresh token por su valor
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Busca un refresh token por ID de usuario
     */
    Optional<RefreshToken> findByUserId(String userId);

    /**
     * Elimina un refresh token por su valor
     */
    void deleteByToken(String token);

    /**
     * Elimina todos los refresh tokens de un usuario
     */
    void deleteByUserId(String userId);

    /**
     * Verifica si existe un refresh token
     */
    boolean existsByToken(String token);
}