package com.saas.gatewayservice.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Filtro global que:
 *   1. Detecta rutas publicas (skip).
 *   2. Valida el JWT firma + expiracion.
 *   3. Verifica blacklist de tokens en Redis (logout). En Phase 7 se publica
 *      el accessToken al hacer logout; aqui ya queda cableada la verificacion.
 *   4. Inyecta {@code X-User-Id}, {@code X-User-Username}, {@code X-User-Roles}
 *      al request forwarded para que los microservicios downstream confien
 *      en la identidad sin re-validar firma.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    public static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final JwtValidator jwt;
    private final RouteValidator routes;
    private final ReactiveStringRedisTemplate redis;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (!routes.requiresAuthentication(request)) {
            return chain.filter(exchange);
        }

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(exchange, "Authorization header ausente o invalido", HttpStatus.UNAUTHORIZED);
        }

        String token = header.substring(7);
        Claims claims = jwt.parse(token).orElse(null);
        if (claims == null) {
            return reject(exchange, "Token invalido o expirado", HttpStatus.UNAUTHORIZED);
        }

        return redis.hasKey(BLACKLIST_KEY_PREFIX + token)
                .defaultIfEmpty(Boolean.FALSE)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return reject(exchange, "Token revocado", HttpStatus.UNAUTHORIZED);
                    }
                    return chain.filter(exchange.mutate()
                            .request(forwardWithIdentity(request, claims))
                            .build());
                })
                .onErrorResume(ex -> {
                    // Si Redis no responde, fallamos de forma segura permitiendo el request
                    // (alternativa: rechazar todo). Logueamos y dejamos pasar.
                    log.warn("Redis blacklist check fallo, dejando pasar token valido: {}", ex.getMessage());
                    return chain.filter(exchange.mutate()
                            .request(forwardWithIdentity(request, claims))
                            .build());
                });
    }

    private ServerHttpRequest forwardWithIdentity(ServerHttpRequest req, Claims claims) {
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        String rolesCsv = roles == null ? "" : String.join(",", roles);

        return req.mutate()
                .header("X-User-Id", userId == null ? "" : userId)
                .header("X-User-Username", username == null ? "" : username)
                .header("X-User-Roles", rolesCsv)
                .build();
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String body = String.format(
                "{\"success\":false,\"status\":%d,\"message\":\"%s\",\"path\":\"%s\",\"timestamp\":\"%s\"}",
                status.value(), message, path, java.time.Instant.now()
        );
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
