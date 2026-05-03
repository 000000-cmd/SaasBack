package com.saas.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicación principal del System Service.
 * Gestiona: Roles, Menús, Permisos, Constantes y sus asignaciones.
 */
@SpringBootApplication(scanBasePackages = {
        "com.saas.system",
        "com.saas.common"
})
@EntityScan(basePackages = {
        "com.saas.system",
        "com.saas.common.outbox"
})
@EnableJpaRepositories(basePackages = {
        "com.saas.system",
        "com.saas.common.outbox"
})
@EnableDiscoveryClient
@EnableScheduling
public class SystemServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemServiceApplication.class, args);
    }
}