package com.saas.auth.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA para Refresh Token.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "auth_refreshtokens")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "UserId", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(name = "Token", nullable = false, unique = true)
    private String token;

    @Column(name = "ExpiryDate", nullable = false)
    private Instant expiryDate;
}
