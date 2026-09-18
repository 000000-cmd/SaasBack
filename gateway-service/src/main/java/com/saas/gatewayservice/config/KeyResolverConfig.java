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

    /**
     * La IP real de quien pide, para contar sus intentos.
     *
     * <p>Detras de Cloudflare Tunnel esto NO es cosmetico. La direccion remota
     * es siempre la del conector, asi que sin cabecera todos los usuarios del
     * mundo compartirian un solo cubo y el primero en pasarse dejaria sin login
     * a los demas.</p>
     *
     * <p>Se prefiere {@code CF-Connecting-IP} sobre {@code X-Forwarded-For}
     * porque la primera la ESCRIBE Cloudflare y sobrescribe lo que mande el
     * cliente; la segunda la APILA, y como aqui se tomaba el primer elemento,
     * bastaba con mandarse uno mismo un {@code X-Forwarded-For} inventado para
     * estrenar cubo en cada intento y saltarse el limite del login.</p>
     */
    private static String clientIp(org.springframework.web.server.ServerWebExchange exchange) {
        String cloudflare = exchange.getRequest().getHeaders().getFirst("CF-Connecting-IP");
        if (cloudflare != null && !cloudflare.isBlank()) {
            return "ip:" + cloudflare.trim();
        }
        // Sin Cloudflare delante (desarrollo, o un proxy propio): el ULTIMO
        // elemento es el que anadio el proxy mas cercano y es el unico que el
        // cliente no pudo escribir.
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            return "ip:" + hops[hops.length - 1].trim();
        }
        InetSocketAddress addr = exchange.getRequest().getRemoteAddress();
        return addr == null ? "ip:unknown" : "ip:" + addr.getAddress().getHostAddress();
    }
}
