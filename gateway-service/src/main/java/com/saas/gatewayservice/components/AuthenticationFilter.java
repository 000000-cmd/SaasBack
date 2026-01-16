package com.saas.gatewayservice.components;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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

@Component
@RefreshScope
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RouteValidator routeValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        log.info("🔍 Gateway Filter -> {} {}", method, path);

        // Verificar si requiere autenticación
        if (!routeValidator.requiresAuthentication(request)) {
            log.info("✅ Public endpoint -> {} {}", method, path);
            return chain.filter(exchange);
        }

        log.info("🔒 Secured endpoint -> {} {}", method, path);

        // Validar presencia del header Authorization
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            log.warn("❌ Missing Authorization header -> {} {}", method, path);
            return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
        }

        // Extraer token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("❌ Invalid Authorization format -> {} {}", method, path);
            return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        log.debug("Token extracted: {}...", token.substring(0, Math.min(20, token.length())));

        // Validar token
        if (!jwtUtil.validateToken(token)) {
            log.warn("❌ Invalid/Expired token -> {} {}", method, path);
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }

        try {
            // Extraer claims
            Claims claims = jwtUtil.extractAllClaims(token);
            String username = claims.getSubject();
            Object userId = claims.get("userId");

            log.info("✅ Token validated -> User: {} (ID: {}) on {} {}", username, userId, method, path);

            // Construir request modificado con headers
            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                    .header("X-User-Username", username);

            if (userId != null) {
                requestBuilder.header("X-User-Id", userId.toString());
            }

            Object roles = claims.get("roles");
            if (roles != null) {
                if (roles instanceof List) {
                    String rolesStr = String.join(",", (List<String>) roles);
                    requestBuilder.header("X-User-Roles", rolesStr);
                    log.debug("User roles: {}", rolesStr);
                } else {
                    requestBuilder.header("X-User-Roles", roles.toString());
                    log.debug("User roles: {}", roles);
                }
            }

            ServerHttpRequest modifiedRequest = requestBuilder.build();
            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.error("❌ Error processing token -> {} {}: {}", method, path, e.getMessage());
            return onError(exchange, "Error processing authentication token", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        String errorBody = String.format(
                "{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d,\"path\":\"%s\",\"method\":\"%s\",\"timestamp\":\"%s\"}",
                status.getReasonPhrase(),
                message,
                status.value(),
                path,
                method,
                java.time.Instant.now().toString()
        );

        byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -1; // Ejecutar antes que otros filtros
    }
}