package com.saas.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Rutas explicitas. Estandar: el path arranca con el nombre del microservicio,
 * que coincide con el {@code server.servlet.context-path} del downstream. El
 * gateway no reescribe; pasa el path tal cual y Spring Boot lo despoja al
 * matchear los controllers.
 *
 *   POST /auth/login              -> auth-service     (rate-limit estricto, key=IP)
 *   /auth/**                       -> auth-service     (rate-limit normal, key=user)
 *   /system/**                     -> system-service   (rate-limit normal, key=user)
 */
@Configuration
public class RouteConfig {

    @Value("${saas.gateway.services.auth-uri:lb://auth-service}")
    private String authUri;

    @Value("${saas.gateway.services.system-uri:lb://system-service}")
    private String systemUri;

    @Bean("loginRateLimiter")
    public RedisRateLimiter loginRateLimiter(
            @Value("${saas.gateway.rate-limit.login.replenish-rate:2}") int replenish,
            @Value("${saas.gateway.rate-limit.login.burst-capacity:5}") int burst) {
        return new RedisRateLimiter(replenish, burst, 1);
    }

    /**
     * @Primary porque {@code RequestRateLimiterGatewayFilterFactory} (autoconfig de
     * Spring Cloud Gateway) inyecta UN unico {@code RateLimiter}. Tener dos beans
     * sin {@code @Primary} causa NoUniqueBeanDefinitionException al arranque.
     * Las rutas que necesitan el de login lo referencian explicitamente por nombre.
     */
    @Bean("defaultRateLimiter")
    @Primary
    public RedisRateLimiter defaultRateLimiter(
            @Value("${saas.gateway.rate-limit.default.replenish-rate:20}") int replenish,
            @Value("${saas.gateway.rate-limit.default.burst-capacity:40}") int burst) {
        return new RedisRateLimiter(replenish, burst, 1);
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               RedisRateLimiter loginRateLimiter,
                               RedisRateLimiter defaultRateLimiter,
                               KeyResolver ipKeyResolver,
                               KeyResolver userKeyResolver) {

        return builder.routes()
                .route("auth-login", r -> r
                        .path("/auth/login")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(loginRateLimiter)
                                .setKeyResolver(ipKeyResolver)))
                        .uri(authUri))
                .route("auth", r -> r
                        .path("/auth/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(defaultRateLimiter)
                                .setKeyResolver(userKeyResolver)))
                        .uri(authUri))
                .route("system", r -> r
                        .path("/system/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(defaultRateLimiter)
                                .setKeyResolver(userKeyResolver)))
                        .uri(systemUri))
                .build();
    }
}
