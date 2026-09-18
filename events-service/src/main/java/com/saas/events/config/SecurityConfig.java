package com.saas.events.config;

import com.saas.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad del events-service. Stateless con JWT.
 *   /actuator/**, /api/info, /api/version -> publico (health del gateway/docker)
 *   /audit/**                              -> autenticado
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Este servicio no tiene context-path: sirve DOS prefijos publicos (/audit y /notification)
                        // sobre el mismo lb://events-service, y Spring Security solo puede matchear el path real
                        // que llega tras el gateway. Por eso hay 4 variantes de /api/info y /api/version (con
                        // ambos prefijos) mas las 2 sin prefijo (llamadas directas al contenedor). No es duplicado.
                        .requestMatchers("/actuator/**", "/api/info", "/api/version",
                                "/audit/api/info", "/audit/api/version",
                                "/notification/api/info", "/notification/api/version").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
