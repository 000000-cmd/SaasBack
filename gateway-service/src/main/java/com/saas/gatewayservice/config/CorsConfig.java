package com.saas.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * CORS centralizado para el gateway.
 *
 * <p>Lee la lista de origenes permitidos desde {@code saas.cors.allowed-origins}
 * (CSV) y los soporta como patrones (permite usar comodines como
 * {@code https://*.tu-dominio.com}). Por defecto habilita los puertos de
 * desarrollo locales del front (Angular dev server).
 *
 * <p>Se registra como {@code @Order(HIGHEST_PRECEDENCE)} para que el filtro CORS
 * corra antes que cualquier otro WebFilter (Spring Security, AuthenticationFilter,
 * rate limiter), garantizando que los preflight {@code OPTIONS} respondan con las
 * cabeceras correctas y nunca lleguen al filtro de autenticacion.
 */
@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter(
            @Value("${saas.cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://127.0.0.1:4200}") String originsCsv,
            @Value("${saas.cors.allow-credentials:true}") boolean allowCredentials,
            @Value("${saas.cors.max-age-seconds:3600}") long maxAge) {

        List<String> origins = Arrays.stream(originsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration cfg = new CorsConfiguration();
        // allowedOriginPatterns acepta tanto valores exactos como patrones (https://*.dominio.com).
        cfg.setAllowedOriginPatterns(origins);
        cfg.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()
        ));
        cfg.setAllowedHeaders(List.of(CorsConfiguration.ALL));
        cfg.setExposedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                "X-User-Id", "X-User-Username", "X-User-Roles",
                "X-Total-Count", "X-Request-Id"
        ));
        cfg.setAllowCredentials(allowCredentials);
        cfg.setMaxAge(Duration.ofSeconds(maxAge));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", cfg);
        return new CorsWebFilter(source);
    }
}
