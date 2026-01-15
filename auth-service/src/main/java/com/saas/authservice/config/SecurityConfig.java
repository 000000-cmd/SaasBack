package com.saas.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitar CSRF ya que usamos JWT (stateless)
                .csrf(csrf -> csrf.disable())

                // IMPORTANTE: Deshabilitar CORS en Spring Security
                // El Gateway ya maneja CORS completamente
                .cors(cors -> cors.disable())

                // Configuración de sesiones: sin estado (stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Deshabilitar HTTP Basic y Form Login
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())

                // Autorización de endpoints
                .authorizeHttpRequests(auth -> auth
                        // Permitir OPTIONS para preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Endpoints públicos de autenticación
                        .requestMatchers("/api/auth/**").permitAll()

                        // Endpoints de actuator (health checks)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Cualquier otra petición requiere autenticación
                        // (será validada por el Gateway mediante JWT)
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
