package com.saas.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Implementacion ligera de {@link IUserPrincipal} para los servicios
 * downstream (auth-service post-login, system-service).
 *
 * Se construye desde los claims del JWT, sin tocar BD.
 */
@Getter
@RequiredArgsConstructor
public class SimpleJwtPrincipal implements IUserPrincipal {

    private final UUID userId;
    private final String username;
    private final Set<String> roles;
}
