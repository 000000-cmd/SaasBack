package com.saas.auth.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Si no hay header de autorización o no empieza con Bearer, continuar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtTokenProvider.extractUsername(jwt);

            // Si tenemos username y no hay autenticación previa
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Validar token
                if (jwtTokenProvider.validateToken(jwt, username)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Usuario autenticado: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("Error procesando token JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        log.debug("Checking if should filter path: {}", path);

        // Paths sin prefijo
        boolean isPublicWithoutPrefix =
                path.startsWith("/api/auth/login") ||
                        path.startsWith("/api/auth/refresh") ||
                        path.startsWith("/api/auth/register") ||
                        path.startsWith("/api/info") ||
                        path.startsWith("/actuator");

        // Paths con prefijo /auth-service (cuando vienen del Gateway)
        boolean isPublicWithPrefix =
                path.startsWith("/auth-service/api/auth/login") ||
                        path.startsWith("/auth-service/api/auth/refresh") ||
                        path.startsWith("/auth-service/api/auth/register") ||
                        path.startsWith("/auth-service/api/info");

        boolean shouldNotFilter = isPublicWithoutPrefix || isPublicWithPrefix;

        log.debug("✅ Path {} - Should NOT filter: {}", path, shouldNotFilter);

        return shouldNotFilter;
    }
}