package com.saas.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Registro de auditoria del sistema. Consume el topic dedicado
 * {@code audit.events} (grupo propio) y persiste cada cambio (quien, que,
 * cuando, antes/despues) en la tabla {@code audit_log}.
 *
 * Es solo-consumidor: no produce eventos ni corre el OutboxRelay.
 */
@SpringBootApplication(scanBasePackages = {
        "com.saas.audit",
        "com.saas.common"
})
@EnableDiscoveryClient
public class AuditServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
