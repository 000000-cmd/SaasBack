package com.saas.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Genera y valida JWTs (HS256). Estructura del token:
 *   - sub        : userId (UUID)
 *   - username   : username
 *   - roles      : Set&lt;String&gt; codigos de rol
 *   - iat / exp  : estandar
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    @Getter private final long accessTokenTtlMillis;
    @Getter private final long refreshTokenTtlMillis;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expirationMs}") long accessTtl,
            @Value("${jwt.refreshTokenExpirationMs}") long refreshTtl
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMillis = accessTtl;
        this.refreshTokenTtlMillis = refreshTtl;
    }

    public String generateAccessToken(UUID userId, String username, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenTtlMillis)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT invalido: {}", ex.getMessage());
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }
}
