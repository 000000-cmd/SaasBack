package com.saas.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Filtro JWT compartido por auth-service y system-service.
 * Valida {@code Authorization: Bearer <token>} y popula el SecurityContext
 * con un {@link SimpleJwtPrincipal} (suficiente para auditoria y
 * autorizacion via {@code @PreAuthorize}).
 *
 * No es necesario consultar BD: todo viene del JWT.
 *
 * Para que se cargue automaticamente en un servicio basta con que su
 * {@code @SpringBootApplication} escanee {@code com.saas.common} y exponga
 * la clave {@code jwt.secret}. La activacion por modulo se hace registrando
 * este filtro en su SecurityConfig (no se cablea solo en la cadena).
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            try {
                Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

                UUID userId = UUID.fromString(c.getSubject());
                String username = c.get("username", String.class);
                @SuppressWarnings("unchecked")
                List<String> rolesList = c.get("roles", List.class);
                Set<String> roles = rolesList == null ? Set.of() : Set.copyOf(rolesList);

                SimpleJwtPrincipal principal = new SimpleJwtPrincipal(userId, username, roles);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principal, null,
                                roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("JWT invalido: {}", ex.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        return (h != null && h.startsWith("Bearer ")) ? h.substring(7) : null;
    }
}
