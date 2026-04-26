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
 * Rutas explicitas (URL limpia, sin prefijo de service-id).
 *
 *   POST /auth/login              -> auth-service     (rate-limit estricto, key=IP)
 *   /auth/**, /users/**           -> auth-service     (rate-limit normal, key=user)
 *   /roles/**, /permissions/**,
 *   /menus/**, /system-lists/**,
 *   /constants/**                 -> system-service   (rate-limit normal, key=user)
 */
@Configuration
public class RouteConfig {

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
                // 1) Login con rate-limit estricto (anti brute-force) + IP key
                .route("auth-login", r -> r
                        .path("/auth/login")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(loginRateLimiter)
                                .setKeyResolver(ipKeyResolver)))
                        .uri("lb://auth-service"))
                // 2) Resto de auth-service (refresh, logout, /users/**)
                .route("auth", r -> r
                        .path("/auth/**", "/users/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(defaultRateLimiter)
                                .setKeyResolver(userKeyResolver)))
                        .uri("lb://auth-service"))
                // 3) System-service (catalogos)
                .route("system", r -> r
                        .path("/roles/**", "/permissions/**", "/menus/**",
                                "/system-lists/**", "/constants/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(defaultRateLimiter)
                                .setKeyResolver(userKeyResolver)))
                        .uri("lb://system-service"))
                .build();
    }
}
