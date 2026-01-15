package com.saas.gatewayservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Aplicación principal del API Gateway
 * Gestiona el enrutamiento y autenticación para todos los microservicios
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(GatewayServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
        log.info("🚀 Gateway Service started successfully");
        log.info("📡 Discovery Client enabled - Connecting to Eureka");
        log.info("🔐 JWT Authentication Filter active");
    }
}
