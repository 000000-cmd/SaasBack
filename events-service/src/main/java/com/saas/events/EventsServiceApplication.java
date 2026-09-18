package com.saas.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de lo que ya ocurrio: lo REGISTRA (auditoria, consumiendo el topic
 * dedicado {@code audit.events}) y lo COMUNICA (notificaciones, consumiendo
 * {@code notification.requested} de {@code domain.events}).
 *
 * Es solo-consumidor: no produce eventos ni corre el OutboxRelay.
 */
@SpringBootApplication(scanBasePackages = {
        "com.saas.events",
        "com.saas.common"
})
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients(basePackages = "com.saas.events.infrastructure.client")
public class EventsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventsServiceApplication.class, args);
    }
}
