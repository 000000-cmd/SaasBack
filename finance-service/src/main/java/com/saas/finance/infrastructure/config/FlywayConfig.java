package com.saas.finance.infrastructure.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Estrategia de arranque de Flyway: ejecuta {@code repair()} antes de
 * {@code migrate()}.
 *
 * Razon: si una migracion previa fallo a mitad (ej. timeout, error de
 * datos), Flyway deja la fila como {@code success=false} en
 * {@code flyway_schema_history} y rechaza arrancar hasta que un humano
 * limpie a mano. {@code repair()} elimina esas filas fallidas y recomputa
 * checksums de las exitosas, asi el reintento es automatico al redeployar
 * con el SQL corregido.
 *
 * Es seguro: no toca migraciones exitosas (solo actualiza checksum si
 * cambio) ni reaplica las que ya estan ok.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
