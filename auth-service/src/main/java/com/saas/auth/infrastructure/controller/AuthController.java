package com.saas.auth.infrastructure.controller;

import com.saas.auth.application.dto.request.LoginRequest;
import com.saas.auth.application.dto.request.RefreshTokenRequest;
import com.saas.auth.application.dto.response.LoginResponse;
import com.saas.auth.application.dto.response.TokenPairResponse;
import com.saas.auth.domain.port.in.IAuthUseCase;
import com.saas.common.dto.ApiResponse;
import com.saas.common.security.IUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthUseCase authUseCase;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authUseCase.login(request), "Login exitoso"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenPairResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authUseCase.refresh(request.refreshToken()), "Token refrescado"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                                     HttpServletRequest http) {
        String refresh = request == null ? null : request.refreshToken();
        String access  = extractBearer(http);
        authUseCase.logout(refresh, access);
        return ResponseEntity.ok(ApiResponse.success(null, "Logout exitoso"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal IUserPrincipal principal) {
        authUseCase.logoutAll(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Sesiones cerradas"));
    }

    private String extractBearer(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        return (h != null && h.startsWith("Bearer ")) ? h.substring(7) : null;
    }
}
