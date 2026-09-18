package com.saas.events.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Estrategia de arranque de Flyway: ejecuta {@code repair()} antes de
 * {@code migrate()}.
 *
 * <p>Razon: si una migracion falla a mitad (un id duplicado, un timeout),
 * Flyway deja la fila como {@code success=false} en
 * {@code flyway_schema_history} y a partir de ahi el servicio NO vuelve a
 * arrancar — ni siquiera con el SQL ya corregido — hasta que un humano borre
 * esa fila a mano. {@code repair()} la elimina y recomputa los checksums de las
 * exitosas, asi que el reintento es automatico al redesplegar.</p>
 *
 * <p>Es seguro: no reaplica migraciones que ya estan en verde.</p>
 *
 * <p>auth-service tenia esto desde el principio; events-service no, y por eso
 * un unico INSERT con un id repetido lo dejaba en bucle de reinicio.</p>
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
