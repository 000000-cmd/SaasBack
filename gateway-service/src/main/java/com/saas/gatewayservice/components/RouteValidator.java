package com.saas.gatewayservice.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Validador de rutas para determinar qué endpoints requieren autenticación
 */
@Component
public class RouteValidator {

    private static final Logger log = LoggerFactory.getLogger(RouteValidator.class);

    /**
     * Lista de endpoints públicos que NO requieren autenticación
     */
    public static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/register",
            "/api/auth/apiV",
            "/api/thirdparties/create",
            "/eureka",
            "/actuator/health",
            "/actuator/info"
    );

    /**
     * Predicado que determina si una ruta está asegurada (requiere auth)
     * Retorna TRUE si la ruta NO está en la lista pública
     */
    public Predicate<ServerHttpRequest> isSecured =
            request -> PUBLIC_ENDPOINTS.stream()
                    .noneMatch(uri -> {
                        String path = request.getURI().getPath();
                        boolean matches = path.contains(uri);
                        if (matches) {
                            log.debug("Public endpoint accessed: {}", path);
                        }
                        return matches;
                    });

    /**
     * Verifica si una ruta específica es pública
     */
    public boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(path::contains);
    }

    /**
     * Verifica si una ruta requiere autenticación
     */
    public boolean requiresAuthentication(ServerHttpRequest request) {
        return isSecured.test(request);
    }
}