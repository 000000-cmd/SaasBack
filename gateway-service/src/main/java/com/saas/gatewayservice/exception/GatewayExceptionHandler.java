package com.saas.gatewayservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Convierte excepciones del gateway en respuestas JSON consistentes.
 * Captura especificamente:
 *   - NotFoundException        -> 503 (servicio no disponible / no registrado en Eureka)
 *   - ResponseStatusException  -> respeta el status original (ej. 429 RateLimit)
 *   - resto                    -> 500
 */
@Slf4j
@Order(-2) // antes del default error handler
@Configuration
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) return Mono.error(ex);

        HttpStatus status;
        String message;

        if (ex instanceof NotFoundException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Servicio no disponible";
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error interno del gateway";
            log.error("Gateway error no manejado", ex);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"success\":false,\"status\":%d,\"message\":\"%s\",\"path\":\"%s\",\"timestamp\":\"%s\"}",
                status.value(), message,
                exchange.getRequest().getURI().getPath(),
                java.time.Instant.now()
        );
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
