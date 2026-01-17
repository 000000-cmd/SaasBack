package com.saas.auth.infrastructure.controller;

import com.saas.auth.application.dto.request.LoginRequest;
import com.saas.auth.application.dto.response.LoginResponse;
import com.saas.auth.application.mapper.UserMapper;
import com.saas.auth.domain.model.User;
import com.saas.auth.domain.port.in.IAuthUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * Controlador REST para autenticación.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthUseCase authUseCase;
    private final UserMapper userMapper;

    @Value("${jwt.refreshExpirationMs:604800000}")
    private long refreshTokenDurationMs;

    @Value("${jwt.cookie.secure:false}")
    private boolean secureCookie;

    /**
     * Endpoint de login.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        log.debug("Solicitud de login para: {}", request.getUsernameOrEmail());

        // Autenticar usuario
        User user = authUseCase.authenticate(request.getUsernameOrEmail(), request.getPassword());

        // Generar tokens
        String accessToken = authUseCase.generateAccessToken(user);
        String refreshToken = authUseCase.createRefreshToken(user.getId());

        // Crear cookie para refresh token
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(secureCookie);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) (refreshTokenDurationMs / 1000));
        response.addCookie(refreshTokenCookie);

        // Construir respuesta
        LoginResponse loginResponse = userMapper.toLoginResponse(user);
        loginResponse.setAccessToken(accessToken);

        return ResponseEntity.ok(ApiResponse.success(loginResponse, "Login exitoso"));
    }

    /**
     * Endpoint para refrescar el access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(HttpServletRequest request) {
        log.debug("Solicitud de refresh token");

        // Obtener refresh token de la cookie
        String refreshToken = getRefreshTokenFromCookies(request);

        if (refreshToken == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("No se encontró token de refresco", 401));
        }

        // Refrescar token
        String newAccessToken = authUseCase.refreshAccessToken(refreshToken);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("accessToken", newAccessToken),
                "Token refrescado exitosamente"
        ));
    }

    /**
     * Endpoint de logout.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        log.debug("Solicitud de logout");

        // Obtener refresh token de la cookie
        String refreshToken = getRefreshTokenFromCookies(request);

        // Invalidar token
        if (refreshToken != null) {
            authUseCase.logout(refreshToken);
        }

        // Eliminar cookie
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.success(null, "Sesión cerrada exitosamente"));
    }

    /**
     * Extrae el refresh token de las cookies.
     */
    private String getRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
