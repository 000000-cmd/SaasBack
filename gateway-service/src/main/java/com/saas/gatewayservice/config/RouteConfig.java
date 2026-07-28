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

    /**
     * Clave de metadatos con la que Spring Cloud Gateway deja anular el
     * {@code httpclient.response-timeout} global en una ruta concreta ({@code -1}
     * = sin limite). La constante de la libreria es interna, asi que se declara
     * aqui con su nombre exacto.
     */
    private static final String RESPONSE_TIMEOUT_ATTR = "response-timeout";

    @Value("${saas.gateway.services.auth-uri:lb://auth-service}")
    private String authUri;

    @Value("${saas.gateway.services.system-uri:lb://system-service}")
    private String systemUri;

    @Value("${saas.gateway.services.search-uri:lb://search-service}")
    private String searchUri;

    @Value("${saas.gateway.services.business-uri:lb://business-service}")
    private String businessUri;

    @Value("${saas.gateway.services.audit-uri:lb://audit-service}")
    private String auditUri;

    @Value("${saas.gateway.services.thirdparty-uri:lb://thirdparty-service}")
    private String thirdpartyUri;

    @Value("${saas.gateway.services.finance-uri:lb://finance-service}")
    private String financeUri;

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
                // El progreso del reindex es un stream (SSE) que queda abierto
                // mientras dure el trabajo. Va ANTES de la ruta general de
                // /search/** y sin el response-timeout global (30s), que lo
                // cortaria a media faena; tampoco pasa por el rate limiter,
                // porque al reconectar varias veces seguidas se autobloquearia.
                .route("search-stream", r -> r
                        .path("/search/admin/reindex/stream")
                        .metadata(RESPONSE_TIMEOUT_ATTR, -1)
                        .uri(searchUri))
                .route("search", r -> r
                        .path("/search/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(defaultRateLimiter)
                                .setKeyResolver(userKeyResolver)))
                        .uri(searchUri))
                .route("business", r -> r
                        .path("/business/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(defaultRateLimiter)
                                .setKeyResolver(userKeyResolver)))
                        .uri(businessUri))
                .route("audit", r -> r
                        .path("/audit/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(defaultRateLimiter)
                                .setKeyResolver(userKeyResolver)))
                        .uri(auditUri))
                .route("thirdparty", r -> r
                        .path("/thirdparty/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(defaultRateLimiter)
                                .setKeyResolver(userKeyResolver)))
                        .uri(thirdpartyUri))
                .route("finance", r -> r
                        .path("/finance/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(defaultRateLimiter)
                                .setKeyResolver(userKeyResolver)))
                        .uri(financeUri))
                .build();
    }
}
