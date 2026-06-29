package com.saas.thirdparty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Microservicio de Terceros. Escanea tambien {@code com.saas.common} para
 * reutilizar seguridad, auditoria y el outbox (relay de eventos a Kafka).
 */
@SpringBootApplication(scanBasePackages = {
        "com.saas.thirdparty",
        "com.saas.common"
})
@EnableDiscoveryClient
@EnableScheduling
@EntityScan(basePackages = {
        "com.saas.thirdparty.infrastructure.persistence.entity",
        "com.saas.common.outbox"
})
@EnableJpaRepositories(basePackages = {
        "com.saas.thirdparty.infrastructure.persistence.repository",
        "com.saas.common.outbox"
})
public class ThirdpartyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThirdpartyServiceApplication.class, args);
    }
}
