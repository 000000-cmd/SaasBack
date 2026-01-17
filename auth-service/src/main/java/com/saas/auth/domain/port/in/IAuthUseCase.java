package com.saas.auth.domain.port.in;

import com.saas.auth.domain.model.User;

/**
 * Puerto de entrada para casos de uso de autenticación.
 */
public interface IAuthUseCase {

    /**
     * Autentica un usuario y genera tokens.
     *
     * @param usernameOrEmail Usuario o email
     * @param password Contraseña en texto plano
     * @return Usuario autenticado
     */
    User authenticate(String usernameOrEmail, String password);

    /**
     * Genera un nuevo access token usando un refresh token.
     *
     * @param refreshToken Token de refresco
     * @return Nuevo access token
     */
    String refreshAccessToken(String refreshToken);

    /**
     * Invalida un refresh token (logout).
     *
     * @param refreshToken Token a invalidar
     */
    void logout(String refreshToken);

    /**
     * Crea un nuevo refresh token para un usuario.
     *
     * @param userId ID del usuario
     * @return Token de refresco generado
     */
    String createRefreshToken(String userId);

    /**
     * Genera un access token JWT para un usuario.
     *
     * @param user Usuario para quien generar el token
     * @return Access token JWT
     */
    String generateAccessToken(User user);
}
