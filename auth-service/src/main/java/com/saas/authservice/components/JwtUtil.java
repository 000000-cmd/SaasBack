package com.saas.authservice.components;

import com.saas.authservice.entities.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMillis;

    // Constructor que inyecta el secreto y la expiración desde application.properties
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expirationMs}") long expirationMillis) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("La clave JWT debe tener mínimo 32 caracteres y no ser nula");
        }
        // Crea la clave segura para firmar y verificar (usando HS256)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    // --- GENERACIÓN DE TOKEN (ACTUALIZADO) ---

    /**
     * Genera un nuevo AccessToken para el usuario, incluyendo roles como claims.
     * @param username El nombre de usuario (subject del token).
     * @param roles El conjunto de UserRole del usuario.
     * @return El token JWT como una cadena.
     */
    public String generateToken(String username, Set<UserRole> roles) {
        Map<String, Object> claims = new HashMap<>();
        // Extraer los códigos de rol y añadirlos como una lista al claim "roles"
        if (roles != null && !roles.isEmpty()) {
            claims.put("roles", roles.stream()
                    // Asegura que no haya nulos intermedios
                    .filter(userRole -> userRole.getRole() != null && userRole.getRole().getCode() != null)
                    .map(userRole -> userRole.getRole().getCode())
                    .collect(Collectors.toList()));
        }
        return createToken(claims, username);
    }

    /**
     * Método helper para construir el token JWT con los claims y el subject dados.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .claims(claims)              // Añade los claims personalizados (ej. roles)
                .subject(subject)            // Establece el username
                .issuedAt(now)               // Fecha de emisión
                .expiration(expiryDate)      // Fecha de expiración
                .signWith(key)               // Firma con la clave (algoritmo detectado automáticamente)
                .compact();
    }

    // --- VALIDACIÓN Y EXTRACCIÓN DE TOKEN (ACTUALIZADO) ---

    /**
     * Extrae TODOS los claims (información) desde un token JWT.
     * Valida la firma en el proceso.
     * @param token El token JWT.
     * @return Un objeto Claims que contiene toda la información del token.
     * @throws JwtException Si el token es inválido o ha sido manipulado.
     */
    public Claims extractAllClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)             // Especifica la clave para verificar la firma
                .build()
                .parseSignedClaims(token)    // Verifica firma y decodifica
                .getPayload();               // Obtiene el payload (claims)
    }

    /**
     * Extrae un claim específico del token usando una función resolver.
     * @param token El token JWT.
     * @param claimsResolver Función para extraer el claim deseado (ej. Claims::getSubject).
     * @return El valor del claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae el nombre de usuario (subject) desde un token JWT.
     * @param token El token JWT.
     * @return El nombre de usuario.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha de expiración desde un token JWT.
     * @param token El token JWT.
     * @return La fecha de expiración.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Verifica si un token JWT ha expirado.
     * @param token El token JWT.
     * @return true si el token ha expirado, false en caso contrario.
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException e) {
            // Si no se puede extraer la expiración, el token es inválido de todos modos
            return true;
        }
    }

    /**
     * Valida si un token es correcto (firma válida Y no expirado).
     * @param token El token JWT a validar.
     * @return true si el token es válido, false en caso contrario (firma inválida, expirado, malformado).
     */
    public boolean validateToken(String token) {
        try {
            // extractAllClaims ya valida la firma. Solo necesitamos chequear la expiración.
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Loggear el error sería bueno aquí para depuración
            // log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}