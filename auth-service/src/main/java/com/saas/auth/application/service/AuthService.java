package com.saas.auth.application.service;

import com.saas.auth.domain.model.RefreshToken;
import com.saas.auth.domain.model.User;
import com.saas.auth.domain.port.in.IAuthUseCase;
import com.saas.auth.domain.port.out.IRefreshTokenRepositoryPort;
import com.saas.auth.domain.port.out.IUserRepositoryPort;
import com.saas.auth.infrastructure.security.JwtTokenProvider;
import com.saas.common.exception.InvalidCredentialsException;
import com.saas.common.exception.TokenRefreshException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Servicio de aplicación para autenticación.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthUseCase {

    private final IUserRepositoryPort userRepository;
    private final IRefreshTokenRepositoryPort refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refreshExpirationMs:604800000}")
    private long refreshTokenDurationMs;

    @Override
    @Transactional(readOnly = true)
    public User authenticate(String usernameOrEmail, String password) {
        log.debug("Autenticando usuario: {}", usernameOrEmail);

        User user = userRepository.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario o contraseña incorrectos"));

        if (!user.getEnabled()) {
            throw new InvalidCredentialsException("Usuario deshabilitado");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }

        log.info("Usuario autenticado exitosamente: {}", user.getUsername());
        return user;
    }

    @Override
    @Transactional
    public String refreshAccessToken(String refreshToken) {
        log.debug("Refrescando access token");

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new TokenRefreshException("Token de refresco no encontrado"));

        if (token.isExpired()) {
            refreshTokenRepository.deleteByToken(refreshToken);
            throw new TokenRefreshException("Token de refresco expirado. Por favor, inicie sesión nuevamente");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new TokenRefreshException("Usuario no encontrado para el token"));

        String newAccessToken = generateAccessToken(user);
        log.info("Access token refrescado para usuario: {}", user.getUsername());

        return newAccessToken;
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        log.debug("Cerrando sesión");

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.deleteByToken(refreshToken);
            log.info("Refresh token eliminado");
        }
    }

    @Override
    @Transactional
    public String createRefreshToken(String userId) {
        log.debug("Creando refresh token para usuario: {}", userId);

        // Eliminar tokens anteriores del usuario
        refreshTokenRepository.deleteByUserId(userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token creado para usuario: {}", userId);

        return saved.getToken();
    }

    @Override
    public String generateAccessToken(User user) {
        return jwtTokenProvider.generateToken(user.getUsername(), user.getRoleCodes());
    }
}