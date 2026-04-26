package com.saas.gatewayservice.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decide si una ruta requiere autenticacion.
 * Las URLs son LIMPIAS (sin prefijo de service-id) gracias a las rutas
 * explicitas declaradas en {@link com.saas.gatewayservice.config.RouteConfig}.
 */
@Component
public class RouteValidator {

    private static final List<String> OPEN_PREFIXES = List.of(
            "/auth/login",
            "/auth/refresh",
            "/actuator",
            "/api/info",
            "/api/version"
    );

    public boolean requiresAuthentication(ServerHttpRequest request) {
        // Preflight CORS siempre publico
        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) return false;

        String path = request.getURI().getPath();
        return OPEN_PREFIXES.stream().noneMatch(path::startsWith);
    }
}
