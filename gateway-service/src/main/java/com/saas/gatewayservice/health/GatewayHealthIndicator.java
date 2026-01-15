package com.saas.gatewayservice.health;

import com.saas.gatewayservice.components.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

/**
 * Indicador de salud personalizado para el Gateway
 * Verifica el estado de componentes críticos
 */
@Component
public class GatewayHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Health health() {
        try {
            // Verificar conexión con Eureka
            if (discoveryClient != null) {
                int serviceCount = discoveryClient.getServices().size();

                if (serviceCount > 0) {
                    return Health.up()
                            .withDetail("jwt", "initialized")
                            .withDetail("eureka", "connected")
                            .withDetail("registered_services", serviceCount)
                            .build();
                } else {
                    return Health.up()
                            .withDetail("jwt", "initialized")
                            .withDetail("eureka", "connected")
                            .withDetail("registered_services", 0)
                            .withDetail("warning", "No services registered yet")
                            .build();
                }
            }

            return Health.up()
                    .withDetail("jwt", "initialized")
                    .withDetail("eureka", "not_configured")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}