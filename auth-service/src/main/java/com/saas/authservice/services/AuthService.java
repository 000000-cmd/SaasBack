package com.saas.authservice.services; // Ajusta tu paquete

import com.saas.authservice.components.JwtUtil;
import com.saas.authservice.dto.internal.ThirdPartyBasicInfoDTO;
import com.saas.authservice.dto.response.LoginResponseDTO;
import com.saas.authservice.entities.RefreshToken;
import com.saas.authservice.entities.User;
import com.saas.authservice.exceptions.InvalidCredentialsException;
import com.saas.authservice.mappers.UserMapper;
import com.saas.authservice.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Slf4j // Añade logging
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final RestTemplate restTemplate;

    // Constructor actualizado
    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       RefreshTokenService refreshTokenService,
                       UserMapper userMapper,
                       RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.restTemplate = restTemplate;
    }

    public LoginResponseDTO login(String usernameOrEmail, String password, HttpServletResponse response) { // Añadido HttpServletResponse
        // 1. Validar credenciales y obtener el usuario
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .filter(u -> passwordEncoder.matches(password, u.getPassword())) // Verifica contraseña aquí
                .orElseThrow(() -> new InvalidCredentialsException("Usuario o contraseña incorrectos."));

        // 2. Llamar al thirdparty-service para obtener el nombre
        ThirdPartyBasicInfoDTO thirdPartyInfo = null; // Inicializa como null
        try {
            // La URL usa el nombre del servicio registrado en Eureka ("thirdparty-service")
            String url = "http://thirdparty-service/api/internal/thirdparties/by-user/" + user.getId();
            thirdPartyInfo = restTemplate.getForObject(url, ThirdPartyBasicInfoDTO.class);
            log.info("Información básica obtenida de thirdparty-service para userId: {}", user.getId());

        } catch (HttpClientErrorException.NotFound ex) {
            // Es normal que no exista un ThirdParty asociado, no hacemos nada, usamos el username
            log.warn("No se encontró ThirdParty para userId: {} en thirdparty-service.", user.getId());
        } catch (RestClientException ex) {
            // Error de comunicación con el otro microservicio
            log.error("Error al intentar comunicar con thirdparty-service para obtener datos de {}: {}", user.getId(), ex.getMessage());
            // Continuamos, pero usaremos el username como nombre
        }

        // 3. Usar el Mapper para construir el DTO base
        LoginResponseDTO responseDTO = userMapper.toLoginResponseDTO(user, thirdPartyInfo);

        // 4. Generar el AccessToken de corta duración
        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRoles()); // Asumiendo que generateToken acepta roles
        responseDTO.setAccessToken(accessToken); // Añadir al cuerpo de la respuesta

        // 5. Crear (o recrear) el RefreshToken de larga duración en la BD
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // 6. Crear la cookie HttpOnly EXCLUSIVAMENTE para el RefreshToken
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken.getToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // Cambiar a true en producción (HTTPS)
        refreshTokenCookie.setPath("/"); // Asegúrate que el path sea correcto
        long duration = refreshToken.getExpiryDate().getEpochSecond() - Instant.now().getEpochSecond();
        refreshTokenCookie.setMaxAge((int) Math.max(0, duration)); // Evita maxAge negativo
        response.addCookie(refreshTokenCookie); // Añadir la cookie a la respuesta HTTP

        return responseDTO;
    }

}

