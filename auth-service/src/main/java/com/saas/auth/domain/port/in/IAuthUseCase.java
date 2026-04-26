package com.saas.auth.domain.port.in;

import com.saas.auth.application.dto.request.LoginRequest;
import com.saas.auth.application.dto.response.LoginResponse;
import com.saas.auth.application.dto.response.TokenPairResponse;

public interface IAuthUseCase {

    LoginResponse login(LoginRequest request);

    TokenPairResponse refresh(String refreshToken);

    /** Revoca el refresh token y publica el access token en blacklist (Redis). */
    void logout(String refreshToken, String accessToken);

    /** Logout global de un usuario (revoca todos sus refresh tokens). */
    void logoutAll(java.util.UUID userId);
}
