package com.saas.authservice.controllers;

import com.saas.authservice.components.JwtUtil;
import com.saas.authservice.dto.request.LoginRequest;
import com.saas.authservice.dto.response.LoginResponseDTO;
import com.saas.authservice.entities.RefreshToken;
import com.saas.authservice.entities.User;
import com.saas.authservice.exceptions.ResourceNotFoundException;
import com.saas.authservice.mappers.UserMapper;
import com.saas.authservice.repositories.UserRepository;
import com.saas.authservice.services.AuthService;
import com.saas.authservice.services.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BuildProperties buildProperties; // Ahora es opcional

    @Autowired
    public AuthController(AuthService authService,
                          RefreshTokenService refreshTokenService,
                          JwtUtil jwtUtil,
                          UserRepository userRepository,
                          UserMapper userMapper,
                          @Autowired(required = false) BuildProperties buildProperties) { // ← CAMBIO AQUÍ
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.buildProperties = buildProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        LoginResponseDTO responseDTO = authService.login(loginRequest.getUsernameOrEmail(), loginRequest.getPassword(), response);
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "No se encontró la cookie de refresco"));
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .flatMap(refreshTokenService::findByToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getRoles());
                    return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("message", "Token de refresco inválido o expirado")));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado para el token principal: " + userDetails.getUsername()));

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("cellular", user.getCellular());
        userInfo.put("attachment", user.getAttachment());
        if (user.getRoles() != null) {
            userInfo.put("roles", user.getRoles().stream()
                    .collect(Collectors.toList()));
        } else {
            userInfo.put("roles", java.util.Collections.emptyList());
        }

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. Invalida el refresh token en la BD si existe
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(cookie -> "refreshToken".equals(cookie.getName()))
                    .findFirst()
                    .ifPresent(cookie -> refreshTokenService.deleteByToken(cookie.getValue()));
        }

        // 2. Expira la cookie en el navegador
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        // cookie.setSecure(true); // Activar en producción (HTTPS)
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "Logout exitoso"));
    }

    @GetMapping("/apiV")
    public Map<String, String> getApiVersion() {
        Map<String, String> response = new HashMap<>();
        // Ahora verifica si buildProperties existe antes de usarlo
        response.put("version", buildProperties != null ? buildProperties.getVersion() : "1.0.0-SNAPSHOT");
        response.put("environment", "development");
        return response;
    }
}