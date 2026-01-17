package com.saas.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Aplicación principal del System Service.
 * Gestiona: Roles, Menús, Permisos, Constantes y sus asignaciones.
 */
@SpringBootApplication(scanBasePackages = {
        "com.saas.system",
        "com.saas.common"
})
@EnableDiscoveryClient
public class SystemServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemServiceApplication.class, args);
    }
}