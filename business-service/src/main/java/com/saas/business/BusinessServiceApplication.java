package com.saas.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicación principal del Business Service.
 * Gestiona: Roles, Menús, Permisos, Constantes y sus asignaciones.
 */
@SpringBootApplication(scanBasePackages = {
        "com.saas.business",
        "com.saas.common"
})
@EntityScan(basePackages = {
        "com.saas.business",
        "com.saas.common.outbox"
})
@EnableJpaRepositories(basePackages = {
        "com.saas.business",
        "com.saas.common.outbox"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.saas.business.infrastructure.client")
@EnableScheduling
public class BusinessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessServiceApplication.class, args);
    }

}
