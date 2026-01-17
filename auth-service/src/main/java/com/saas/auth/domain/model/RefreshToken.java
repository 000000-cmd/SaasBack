package com.saas.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Modelo de dominio para Refresh Token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    private Long id;
    private String userId;
    private String token;
    private Instant expiryDate;

    /**
     * Verifica si el token ha expirado.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }
}