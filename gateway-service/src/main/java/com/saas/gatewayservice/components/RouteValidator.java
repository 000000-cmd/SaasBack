package com.saas.gatewayservice.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RouteValidator {

    private static final Logger log = LoggerFactory.getLogger(RouteValidator.class);

    /**
     * Lista de paths públicos que NO requieren autenticación
     */
    public static final List<String> PUBLIC_ENDPOINTS = List.of(
            // Auth service - login, register, refresh
            "/auth-service/api/auth/login",
            "/auth-service/api/auth/refresh",
            "/auth-service/api/auth/register",
            "/auth-service/api/info",
            "/auth-service/api/version",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/register",

            // Actuator endpoints
            "/actuator/health",
            "/actuator/info",
            "/auth-service/actuator/health",
            "/auth-service/actuator/info",
            "/system-service/actuator/health",
            "/system-service/actuator/info",
            "/gateway-service/actuator/health",
            "/gateway-service/actuator/info",

            // System-service
            "/system-service/api/info",
            "/system-service/api/version",
            "/api/info",
            "/api/version",

            // Eureka
            "/eureka"
    );

    /**
     * Verifica si una ruta requiere autenticación
     */
    public boolean requiresAuthentication(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // Permitir siempre OPTIONS para CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.debug("OPTIONS request detected - allowing without auth: {}", path);
            return false;
        }

        // Verificar si es un endpoint público
        boolean isPublic = PUBLIC_ENDPOINTS.stream()
                .anyMatch(publicPath -> path.equals(publicPath) || path.startsWith(publicPath));

        if (isPublic) {
            log.debug("Public endpoint detected: {} {}", method, path);
            return false;
        }

        log.debug("Secured endpoint detected: {} {}", method, path);
        return true;
    }

    /**
     * Verifica si una ruta específica es pública
     */
    public boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(publicPath -> path.equals(publicPath) || path.startsWith(publicPath));
    }
}