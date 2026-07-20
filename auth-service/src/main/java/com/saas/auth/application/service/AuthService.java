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
import com.saas.auth.infrastructure.client.SearchServiceClient;
import com.saas.auth.infrastructure.client.ThirdPartyServiceClient;
import com.saas.auth.infrastructure.security.BusinessResolver;
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
import java.util.Optional;
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
    private final BusinessResolver businessResolver;
    private final ThirdPartyServiceClient thirdPartyClient;
    private final SearchServiceClient searchClient;

    /**
     * Id fijo y conocido del rol {@code OWNER} (sembrado en la migración V1).
     * Se referencia directamente para evitar una resolución code→id por Feign
     * a system-service en el alta de un dueño.
     */
    private static final UUID OWNER_ROLE_ID = UUID.fromString("11111111-0000-0000-0000-000000000004");

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return email != null && !email.isBlank() && userRepo.existsByEmail(email.trim());
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Login flexible: username, correo o numero de documento (comodidad del
        // APK). El documento se resuelve via thirdparty solo si no hay match
        // directo y el identificador es puramente numerico.
        User user = userRepo.findByUsernameOrEmail(request.usernameOrEmail())
                .or(() -> findByDocumentNumber(request.usernameOrEmail()))
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales invalidas"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new InvalidCredentialsException("Cuenta deshabilitada");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenciales invalidas");
        }

        // Cargar roles efectivos para el JWT
        User withRoles = userUseCase.loadWithRoles(user.getId());

        // Resolver el negocio del dueño una sola vez: se sella en el token y se
        // expone en la respuesta (evita el doble lookup).
        UUID businessId = businessResolver.resolve(withRoles.getId());
        TokenPairResponse tokens = issueTokens(withRoles, businessId);

        // Actualizar ultimo login (sin tocar otros campos)
        withRoles.setLastLoginAt(LocalDateTime.now());
        userRepo.update(withRoles);

        UserResponse userResponse = toUserResponseWithRoles(withRoles, businessId);
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

        log.info("Registro de dueño: userId={} username={}", created.getId(), created.getUsername());

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

        // Rotacion: revoca el viejo y emite un nuevo par. Re-resolvemos el negocio
        // para que un dueño recién aprovisionado obtenga el claim en el refresh.
        refreshTokenRepo.revokeByToken(refreshTokenValue);
        UUID businessId = businessResolver.resolve(user.getId());
        return issueTokens(user, businessId);
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

    /**
     * Resuelve la cuenta por numero de documento (solo identificadores 100%
     * numericos de tamano plausible). Va primero al read model de Elasticsearch
     * y solo cae a thirdparty-service si ES no lo resuelve (documento recien
     * creado y aun no proyectado). NUNCA rompe el login: ante cualquier fallo
     * devuelve empty y el flujo termina en credenciales invalidas.
     */
    private Optional<User> findByDocumentNumber(String identifier) {
        String id = identifier == null ? "" : identifier.trim();
        if (!id.matches("\\d{5,20}")) return Optional.empty();

        UUID userId = resolveUserIdFromSearch(id);
        if (userId == null) userId = resolveUserIdFromThirdParty(id);
        return userId == null ? Optional.empty() : userRepo.findById(userId);
    }

    /** Read model (rapido). Null si ES no responde o no lo tiene proyectado. */
    private UUID resolveUserIdFromSearch(String documentNumber) {
        try {
            SearchServiceClient.UserByDocumentDto dto = searchClient.userByDocument(documentNumber);
            return dto == null ? null : dto.userId();
        } catch (Exception ex) {
            log.debug("ES no resolvio el documento '{}', respaldo a thirdparty: {}", documentNumber, ex.getMessage());
            return null;
        }
    }

    /** Fuente de verdad (respaldo). */
    private UUID resolveUserIdFromThirdParty(String documentNumber) {
        try {
            ThirdPartyServiceClient.UserByDocumentDto dto = thirdPartyClient.userByDocument(documentNumber);
            return dto == null ? null : dto.userId();
        } catch (Exception ex) {
            log.debug("Login por documento sin match para '{}': {}", documentNumber, ex.getMessage());
            return null;
        }
    }

    private TokenPairResponse issueTokens(User user, UUID businessId) {
        String access = jwt.generateAccessToken(user.getId(), user.getUsername(), user.getRoleCodes(), businessId);

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

    private UserResponse toUserResponseWithRoles(User user, UUID businessId) {
        UserResponse base = userMapper.toResponse(user);
        return new UserResponse(
                base.id(), base.username(), base.email(), base.firstName(), base.lastName(),
                base.fullName(), base.profilePhoto(), base.theme(), base.languageCode(),
                base.lastLoginAt(), base.isFirstLogin(), base.enabled(), base.visible(),
                user.getRoleCodes(),
                base.createdDate(),
                businessId
        );
    }
}
