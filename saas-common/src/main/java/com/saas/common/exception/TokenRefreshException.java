package com.saas.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para tokens de refresco inválidos o expirados.
 * Retorna HTTP 403 Forbidden.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenRefreshException extends RuntimeException {

    public TokenRefreshException(String token, String message) {
        super(String.format("Token [%s]: %s", token, message));
    }

    public TokenRefreshException(String message) {
        super(message);
    }
}