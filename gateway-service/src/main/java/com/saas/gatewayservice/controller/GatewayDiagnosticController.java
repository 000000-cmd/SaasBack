package com.saas.gatewayservice.controller;

import com.saas.gatewayservice.components.JwtUtil;
import com.saas.gatewayservice.components.RouteValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/gateway")
public class GatewayDiagnosticController {

    @Autowired
    private RouteValidator routeValidator;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Verificar si una ruta es pública
     */
    @GetMapping("/check-route")
    public ResponseEntity<Map<String, Object>> checkRoute(@RequestParam String path) {
        Map<String, Object> response = new HashMap<>();
        response.put("path", path);
        response.put("isPublic", routeValidator.isPublicEndpoint(path));
        response.put("requiresAuth", !routeValidator.isPublicEndpoint(path));
        return ResponseEntity.ok(response);
    }

    /**
     * Listar servicios registrados
     */
    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> getServices() {
        List<String> services = discoveryClient.getServices();
        Map<String, Object> response = new HashMap<>();
        response.put("services", services);
        response.put("count", services.size());

        Map<String, List<String>> serviceInstances = services.stream()
                .collect(Collectors.toMap(
                        service -> service,
                        service -> discoveryClient.getInstances(service)
                                .stream()
                                .map(instance -> instance.getUri().toString())
                                .collect(Collectors.toList())
                ));

        response.put("instances", serviceInstances);
        return ResponseEntity.ok(response);
    }

    /**
     * Verificar configuración JWT
     */
    @GetMapping("/jwt-config")
    public ResponseEntity<Map<String, Object>> getJwtConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("secretConfigured", jwtSecret != null && !jwtSecret.isEmpty());
        response.put("secretLength", jwtSecret != null ? jwtSecret.length() : 0);
        response.put("secretValid", jwtSecret != null && jwtSecret.length() >= 32);
        return ResponseEntity.ok(response);
    }

    /**
     * Listar rutas públicas
     */
    @GetMapping("/public-routes")
    public ResponseEntity<Map<String, Object>> getPublicRoutes() {
        Map<String, Object> response = new HashMap<>();
        response.put("publicEndpoints", RouteValidator.PUBLIC_ENDPOINTS);
        response.put("count", RouteValidator.PUBLIC_ENDPOINTS.size());
        return ResponseEntity.ok(response);
    }
}