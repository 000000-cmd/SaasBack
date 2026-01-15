package com.saas.gatewayservice.components;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

@Component
@RefreshScope
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/apiV",
            "/api/thirdparties/create",
            "/eureka"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Lógica: Si la URL coincide con alguna pública, isApiSecured será FALSE
        Predicate<ServerHttpRequest> isApiSecured = r -> openApiEndpoints.stream()
                .noneMatch(uri -> r.getURI().getPath().contains(uri));

        // Si es segura (no está en la lista pública), validamos token
        if (isApiSecured.test(request)) {
            if (!request.getHeaders().containsKey("Authorization")) {
                return this.onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            final String token = this.getAuthHeader(request);

            if (!jwtUtil.validateToken(token)) {
                return this.onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);
            }

            // Token válido, extraemos usuario y seguimos
            Claims claims = jwtUtil.extractAllClaims(token);
            exchange.getRequest().mutate()
                    .header("X-User-Username", claims.getSubject())
                    .build();
        }

        return chain.filter(exchange);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    private String getAuthHeader(ServerHttpRequest request) {
        return request.getHeaders().getOrEmpty("Authorization").get(0).substring(7);
    }

    // Define la prioridad: -1 hace que se ejecute muy temprano
    @Override
    public int getOrder() {
        return -1;
    }
}