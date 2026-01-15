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

/**
 * Filtro global de autenticación para Spring Cloud Gateway
 * Valida tokens JWT en todas las peticiones excepto endpoints públicos
 */
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

        log.debug("Processing request to: {}", path);

        // Si es una ruta pública, continuar sin validación
        if (!routeValidator.requiresAuthentication(request)) {
            log.debug("Public endpoint accessed, skipping authentication: {}", path);
            return chain.filter(exchange);
        }

        // Validar presencia del header Authorization
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            log.warn("Missing Authorization header for secured endpoint: {}", path);
            return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
        }

        // Extraer token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Invalid Authorization header format: {}", authHeader);
            return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Validar token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid or expired token for path: {}", path);
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }

        try {
            // Extraer claims y agregar headers personalizados
            Claims claims = jwtUtil.extractAllClaims(token);
            String username = claims.getSubject();

            log.debug("Token validated successfully for user: {}", username);

            // Construir el request modificado con headers personalizados
            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                    .header("X-User-Username", username);

            // Agregar userId si existe
            Object userId = claims.get("userId");
            if (userId != null) {
                requestBuilder.header("X-User-Id", userId.toString());
            }

            // Agregar roles si existen (puede ser una lista)
            Object roles = claims.get("roles");
            if (roles != null) {
                if (roles instanceof List) {
                    // Si es una lista, convertir a string separado por comas
                    String rolesStr = String.join(",", (List<String>) roles);
                    requestBuilder.header("X-User-Roles", rolesStr);
                } else {
                    // Si es un string simple
                    requestBuilder.header("X-User-Roles", roles.toString());
                }
            }

            ServerHttpRequest modifiedRequest = requestBuilder.build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.error("Error processing token: {}", e.getMessage(), e);
            return onError(exchange, "Error processing authentication token", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Maneja errores de autenticación con respuesta JSON
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorBody = String.format(
                "{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d}",
                status.getReasonPhrase(),
                message,
                status.value()
        );

        byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    /**
     * Prioridad del filtro: -1 para ejecutarse antes que otros filtros
     */
    @Override
    public int getOrder() {
        return -1;
    }
}