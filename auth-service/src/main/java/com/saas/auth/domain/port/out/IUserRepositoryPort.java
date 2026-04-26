package com.saas.auth.domain.port.out;

import com.saas.auth.domain.model.User;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para User.
 *
 * Hereda CRUD por Id de {@link IGenericRepositoryPort}, y agrega busquedas
 * por las claves de negocio (username, email).
 */
public interface IUserRepositoryPort extends IGenericRepositoryPort<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    /** Login flexible: acepta username o email. */
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
