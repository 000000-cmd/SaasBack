package com.saas.common.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Lee el header opcional {@code X-Business-Id} y lo expone via {@link BusinessContext}
 * durante el request. Si no viene, el contexto queda null (evento a nivel sistema).
 * Solo aplica en apps servlet (no en el gateway reactivo).
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class BusinessContextFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Business-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String raw = request.getHeader(HEADER);
            if (raw != null && !raw.isBlank()) {
                try {
                    BusinessContext.set(UUID.fromString(raw.trim()));
                } catch (IllegalArgumentException ignored) {
                    // header malformado: se ignora, queda null
                }
            }
            chain.doFilter(request, response);
        } finally {
            BusinessContext.clear();
        }
    }
}
