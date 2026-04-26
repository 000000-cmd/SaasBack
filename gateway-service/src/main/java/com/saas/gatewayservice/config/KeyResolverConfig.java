package com.saas.gatewayservice.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class KeyResolverConfig {

    /**
     * Por IP. Usado para rutas publicas como {@code /auth/login} (no hay JWT aun).
     */
    @Bean("ipKeyResolver")
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(clientIp(exchange));
    }

    /**
     * Por usuario (X-User-Id ya inyectado por AuthenticationFilter); fallback a IP.
     * Usado para rutas autenticadas para que los limites sean por sesion, no
     * por nodo NAT.
     */
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just((userId != null && !userId.isBlank()) ? "u:" + userId : clientIp(exchange));
        };
    }

    private static String clientIp(org.springframework.web.server.ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        InetSocketAddress addr = exchange.getRequest().getRemoteAddress();
        return addr == null ? "ip:unknown" : "ip:" + addr.getAddress().getHostAddress();
    }
}
