package com.saas.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicación principal del Finance Service.
 *
 * <p>Servicio financiero de la plataforma. Gobierna su propio esquema MySQL
 * dedicado ({@code saas_finance}) mediante Flyway al arranque, igual que
 * auth-service y audit-service gobiernan los suyos.</p>
 *
 * <p>Es productor de eventos de dominio: escribe en {@code outbox_event}
 * (mismo esquema) dentro de la transacción del cambio y el
 * {@code OutboxRelay} de saas-common los publica a Kafka
 * ({@code @EnableScheduling} habilita el poller del relay).</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.saas.finance",
        "com.saas.common"
})
@EntityScan(basePackages = {
        "com.saas.finance",
        "com.saas.common.outbox"
})
@EnableJpaRepositories(basePackages = {
        "com.saas.finance",
        "com.saas.common.outbox"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.saas.finance.infrastructure.client")
@EnableScheduling
public class FinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }

}
