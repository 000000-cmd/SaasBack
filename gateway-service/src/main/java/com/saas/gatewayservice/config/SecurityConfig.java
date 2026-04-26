package com.saas.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security en el gateway esta deshabilitado para todas las rutas:
 * la autenticacion la maneja {@code AuthenticationFilter} (GlobalFilter)
 * que es mas adecuado para Cloud Gateway. Aqui solo evitamos que el
 * autoconfig por defecto pida basic auth.
 *
 * <p>{@code .cors(Customizer.withDefaults())} delega el manejo de CORS al bean
 * {@code CorsWebFilter} declarado en {@link CorsConfig}, garantizando que los
 * preflight {@code OPTIONS} respondan correctamente sin pasar por la cadena de
 * autenticacion.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .cors(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .build();
    }
}
