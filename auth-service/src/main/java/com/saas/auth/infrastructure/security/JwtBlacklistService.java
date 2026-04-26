package com.saas.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * Publica access tokens revocados en Redis con TTL = remaining lifetime.
 * El gateway consulta {@code jwt:blacklist:<token>} en cada request: si existe,
 * 401 sin tocar downstream. Una vez expira el token natural, Redis lo borra
 * solo (sin job de limpieza).
 *
 * El prefijo coincide con {@link com.saas.gatewayservice.security.AuthenticationFilter#BLACKLIST_KEY_PREFIX}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    public static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redis;
    private final JwtTokenProvider jwt;

    public void blacklist(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return;
        try {
            Claims claims = jwt.parse(accessToken);
            Date exp = claims.getExpiration();
            long remainingMs = exp.getTime() - System.currentTimeMillis();
            if (remainingMs <= 0) return; // ya expirado, no vale gastar memoria en Redis
            redis.opsForValue().set(PREFIX + accessToken, "1", Duration.ofMillis(remainingMs));
            log.debug("Token blacklisted, TTL {}ms", remainingMs);
        } catch (Exception ex) {
            // Si Redis cae o token mal formado, NO bloqueamos el logout
            log.warn("No se pudo blacklistar token (se depende solo de la revocacion del refresh): {}", ex.getMessage());
        }
    }
}
