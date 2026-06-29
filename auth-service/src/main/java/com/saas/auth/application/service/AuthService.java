package com.saas.auth.application.service;

import com.saas.auth.application.dto.request.LoginRequest;
import com.saas.auth.application.dto.request.RegisterOwnerRequest;
import com.saas.auth.application.dto.response.LoginResponse;
import com.saas.auth.application.dto.response.TokenPairResponse;
import com.saas.auth.application.dto.response.UserResponse;
import com.saas.auth.application.mapper.UserMapper;
import com.saas.auth.domain.model.RefreshToken;
import com.saas.auth.domain.model.User;
import com.saas.auth.domain.port.in.IAuthUseCase;
import com.saas.auth.domain.port.in.IUserUseCase;
import com.saas.auth.domain.port.out.IRefreshTokenRepositoryPort;
import com.saas.auth.domain.port.out.IUserRepositoryPort;
import com.saas.auth.infrastructure.security.JwtBlacklistService;
import com.saas.auth.infrastructure.security.JwtTokenProvider;
import com.saas.common.exception.InvalidCredentialsException;
import com.saas.common.exception.TokenRefreshException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Casos de uso de autenticacion: login, refresh y logout.
 * Logout en Phase 5: revoca el refresh token. La blacklist del access token
 * en Redis se anade en Phase 7.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthUseCase {

    private final IUserRepositoryPort userRepo;
    private final IUserUseCase userUseCase;
    private final IRefreshTokenRepositoryPort refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final JwtBlacklistService blacklist;
    private final UserMapper userMapper;

    /**
     * Id fijo y conocido del rol {@code OWNER} (sembrado en la migración V1).
     * Se referencia directamente para evitar una resolución code→id por Feign
     * a system-service en el alta de un dueño.
     */
    private static final UUID OWNER_ROLE_ID = UUID.fromString("11111111-0000-0000-0000-000000000004");

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepo.findByUsernameOrEmail(request.usernameOrEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales invalidas"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new InvalidCredentialsException("Cuenta deshabilitada");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenciales invalidas");
        }

        // Cargar roles efectivos para el JWT
        User withRoles = userUseCase.loadWithRoles(user.getId());

        TokenPairResponse tokens = issueTokens(withRoles);

        // Actualizar ultimo login (sin tocar otros campos)
        withRoles.setLastLoginAt(LocalDateTime.now());
        userRepo.update(withRoles);

        UserResponse userResponse = toUserResponseWithRoles(withRoles);
        log.info("Login exitoso: userId={} username={}", withRoles.getId(), withRoles.getUsername());
        return new LoginResponse(tokens, userResponse);
    }

    @Override
    @Transactional
    public LoginResponse registerOwner(RegisterOwnerRequest request) {
        // 1) Crear la cuenta del dueño (reusa el flujo de creación con password).
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();
        User created = userUseCase.createWithPassword(user, request.password());

        // 2) Asignar el rol OWNER (id fijo sembrado).
        userUseCase.assignRoles(created.getId(), Set.of(OWNER_ROLE_ID));

        log.info("Registro de dueño: userId={} username={} negocio='{}' slug='{}'",
                created.getId(), created.getUsername(), request.businessName(), request.slug());

        // 3) Devolver la sesión iniciada (mismos tokens que el login).
        return login(new LoginRequest(request.email(), request.password()));
    }

    @Override
    @Transactional
    public TokenPairResponse refresh(String refreshTokenValue) {
        RefreshToken token = refreshTokenRepo.findByToken(refreshTokenValue)
                .orElseThrow(() -> new TokenRefreshException("Refresh token no encontrado"));

        if (!token.isUsable()) {
            throw new TokenRefreshException("Refresh token expirado o revocado");
        }

        User user = userUseCase.loadWithRoles(token.getUserId());

        // Rotacion: revoca el viejo y emite un nuevo par
        refreshTokenRepo.revokeByToken(refreshTokenValue);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(String refreshTokenValue, String accessToken) {
        if (refreshTokenValue != null) {
            refreshTokenRepo.revokeByToken(refreshTokenValue);
        }
        if (accessToken != null) {
            blacklist.blacklist(accessToken);
        }
        log.info("Logout completado: refresh revocado + access blacklisted");
    }

    @Override
    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenRepo.revokeAllByUserId(userId);
        log.info("Logout global: userId={}", userId);
    }

    private TokenPairResponse issueTokens(User user) {
        String access = jwt.generateAccessToken(user.getId(), user.getUsername(), user.getRoleCodes());

        String refreshValue = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        long refreshTtlMs = jwt.getRefreshTokenTtlMillis();

        RefreshToken refresh = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshValue)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTtlMs / 1000))
                .build();
        refreshTokenRepo.save(refresh);

        return TokenPairResponse.bearer(access, refreshValue, jwt.getAccessTokenTtlMillis() / 1000);
    }

    private UserResponse toUserResponseWithRoles(User user) {
        UserResponse base = userMapper.toResponse(user);
        return new UserResponse(
                base.id(), base.username(), base.email(), base.firstName(), base.lastName(),
                base.fullName(), base.profilePhoto(), base.theme(), base.languageCode(),
                base.lastLoginAt(), base.enabled(), base.visible(),
                user.getRoleCodes(),
                base.createdDate()
        );
    }
}
