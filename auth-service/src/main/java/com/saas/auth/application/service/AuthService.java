package com.saas.auth.application.service;

import com.saas.auth.application.dto.request.LoginRequest;
import com.saas.auth.application.dto.response.DeviceConflictResponse.Conflict;
import com.saas.auth.application.exception.DeviceConflictException;
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
import java.util.List;
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
    private final DeviceLinkService deviceLinks;

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

        // SEPARACION ESTRICTA DE ENTRADAS. Se comprueba ANTES de emitir tokens:
        // antes el login triunfaba, se firmaban los tokens y era el navegador
        // quien descartaba la sesion — con lo que cualquiera que llamase a la
        // API directamente se quedaba con un token valido.
        //
        // El error es LITERALMENTE el mismo que el de un usuario inexistente. No
        // se dice "usa tu acceso dedicado" ni nada parecido: eso confirmaria que
        // la cuenta existe Y que es de administrador, que es justo lo que un
        // atacante quiere averiguar.
        // EL EMPLEADO TAMPOCO ENTRA POR LA WEB. Su sitio es el APK, y esto
        // estaba comprobado SOLO en la pantalla de login: el servidor firmaba
        // los tokens y era el navegador quien los tiraba a la basura. Quien
        // llamara a la API directamente se quedaba con una sesion valida y con
        // un menu de panel — y cada pantalla de ese panel le respondia 403.
        // Es exactamente el fallo que ya se arreglo aqui para los
        // administradores, olvidado en el otro caso.
        //
        // Aqui SI se dice el motivo, al reves que con los administradores: que
        // una cuenta sea de empleado no es ningun secreto —su propio dueno lo
        // sabe— y callarselo solo consigue que se quede mirando la pantalla.
        // Ademas a este punto solo se llega con la contrasena correcta.
        if (esEmpleadoDeAPK(request.surface(), withRoles)) {
            log.info("Login de empleado rechazado en la web: username={}",
                    withRoles.getUsername());
            throw new org.springframework.security.access.AccessDeniedException(
                    "Las cuentas de empleado ingresan por la app móvil.");
        }

        if (!surfaceAllows(request.surface(), withRoles)) {
            log.info("Login rechazado por superficie: username={} surface={}",
                    withRoles.getUsername(), request.surface());
            throw new InvalidCredentialsException("Credenciales invalidas");
        }

        // UNA CUENTA, UN APARATO. Se comprueba DESPUES de validar la contrasena
        // (si no, cualquiera podria averiguar desde donde entra otra persona
        // probando identificadores) y ANTES de emitir tokens: si la persona
        // cancela el aviso, no puede quedarle una sesion viva a medias.
        List<Conflict> choques = deviceLinks.conflicts(withRoles.getId(), request.device());
        if (!choques.isEmpty() && !request.unlinkOthers()) {
            log.info("Login con sesion abierta en otro sitio: userId={} choques={}",
                    withRoles.getId(), choques.size());
            throw new DeviceConflictException(mensajeChoque(choques), choques);
        }

        // Resolver el negocio del dueño una sola vez: se sella en el token y se
        // expone en la respuesta (evita el doble lookup).
        UUID businessId = businessResolver.resolve(withRoles.getId());
        TokenPairResponse tokens = issueTokens(withRoles, businessId);

        // Actualizar ultimo login (sin tocar otros campos)
        withRoles.setLastLoginAt(LocalDateTime.now());
        userRepo.update(withRoles);

        // Vincular el aparato. Va DESPUES de emitir los tokens: si desvincula a
        // otro, lo que se revoca son las sesiones viejas, no la que se acaba de
        // firmar aqui.
        deviceLinks.bind(withRoles, request.device(), request.unlinkOthers());

        UserResponse userResponse = toUserResponseWithRoles(withRoles, businessId);
        log.info("Login exitoso: userId={} username={}", withRoles.getId(), withRoles.getUsername());
        return new LoginResponse(tokens, userResponse);
    }

    /**
     * El texto que ve la persona. Se distingue el caso porque son dos sustos
     * distintos: "mi cuenta esta en otro telefono" y "este telefono tiene otra
     * cuenta" no se responden igual.
     */
    private static String mensajeChoque(List<Conflict> choques) {
        boolean otroAparato = choques.stream().anyMatch(c -> Conflict.OTHER_DEVICE.equals(c.kind()));
        boolean otraCuenta = choques.stream().anyMatch(c -> Conflict.OTHER_ACCOUNT.equals(c.kind()));
        if (otroAparato && otraCuenta) {
            return "Tu cuenta está abierta en otro dispositivo y en este hay otra sesión iniciada.";
        }
        if (otroAparato) {
            return "Tu cuenta ya está abierta en otro dispositivo.";
        }
        return "En este dispositivo hay otra cuenta con la sesión abierta.";
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

    /**
     * Roles que hacen a alguien administrador del sistema. Coincide con lo que
     * el front resuelve como {@code SYSTEM_ADMIN}; si aquí y allí divergieran,
     * la separación de entradas dejaría de tener sentido.
     */
    private static final java.util.Set<String> SYSTEM_ADMIN_ROLES =
            java.util.Set.of("ADMIN", "SUPER_ADMIN", "SYSTEM_ADMIN");

    /**
     * ¿Puede este usuario entrar por esta puerta?
     *
     *   ADMIN   (:4201) — SOLO administradores.
     *   PRODUCT (:4200) — todos MENOS administradores.
     *   sin superficie  — sin restricción (el APK y los llamadores internos).
     *
     * La regla va en los dos sentidos a propósito: si :4201 admitiera a un
     * usuario normal, dejaría de ser "estrictamente para administración".
     */
    /**
     * Un empleado intentando entrar por la web.
     *
     * <p>Si ademas es dueno, entra: hay quien atiende en su propio local, y
     * negarle su panel por tener tambien ficha de empleado seria absurdo.</p>
     *
     * <p>Sin superficie no se rechaza: el APK no manda ninguna, y es justo
     * quien tiene que poder entrar.</p>
     */
    private boolean esEmpleadoDeAPK(String surface, User user) {
        return LoginRequest.SURFACE_PRODUCT.equalsIgnoreCase(surface)
                && user.hasRole("EMPLOYEE") && !user.hasRole("OWNER");
    }

    private boolean surfaceAllows(String surface, User user) {
        if (surface == null || surface.isBlank()) return true;

        boolean esAdmin = SYSTEM_ADMIN_ROLES.stream().anyMatch(user::hasRole);

        if (LoginRequest.SURFACE_ADMIN.equalsIgnoreCase(surface))   return esAdmin;
        if (LoginRequest.SURFACE_PRODUCT.equalsIgnoreCase(surface)) return !esAdmin;

        // Superficie desconocida: no se adivina. Mejor negar que abrir de más.
        return false;
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
