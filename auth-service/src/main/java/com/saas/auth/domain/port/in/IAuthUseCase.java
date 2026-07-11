package com.saas.auth.domain.port.in;

import com.saas.auth.application.dto.request.LoginRequest;
import com.saas.auth.application.dto.request.RegisterOwnerRequest;
import com.saas.auth.application.dto.response.LoginResponse;
import com.saas.auth.application.dto.response.TokenPairResponse;

public interface IAuthUseCase {

    LoginResponse login(LoginRequest request);

    /**
     * Alta self-service de un dueño: crea la cuenta con rol OWNER y devuelve la
     * sesión ya iniciada (mismos tokens que el login).
     */
    LoginResponse registerOwner(RegisterOwnerRequest request);

    /**
     * ¿El correo ya está registrado? Público, para validar disponibilidad en el
     * primer paso del registro antes de enviar (solo lectura).
     */
    boolean emailExists(String email);

    TokenPairResponse refresh(String refreshToken);

    /** Revoca el refresh token y publica el access token en blacklist (Redis). */
    void logout(String refreshToken, String accessToken);

    /** Logout global de un usuario (revoca todos sus refresh tokens). */
    void logoutAll(java.util.UUID userId);
}
