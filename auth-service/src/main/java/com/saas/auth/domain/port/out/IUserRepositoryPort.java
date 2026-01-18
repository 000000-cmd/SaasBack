package com.saas.auth.domain.port.out;

import com.saas.auth.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de usuarios.
 *
 * NOTA: Esta interface NO extiende IGenericRepositoryPort porque
 * User no usa código (code) como identificador de negocio,
 * usa username y email.
 */
public interface IUserRepositoryPort {

    /**
     * Guarda un usuario
     */
    User save(User user);

    /**
     * Actualiza un usuario
     */
    User update(User user);

    /**
     * Busca un usuario por ID
     */
    Optional<User> findById(String id);

    /**
     * Busca un usuario por nombre de usuario
     */
    Optional<User> findByUsername(String username);

    /**
     * Busca un usuario por email
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca un usuario por nombre de usuario o email
     */
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    /**
     * Obtiene todos los usuarios visibles
     */
    List<User> findAll();

    /**
     * Obtiene todos los usuarios incluyendo los no visibles
     */
    List<User> findAllIncludingHidden();

    /**
     * Verifica si existe un usuario con el nombre de usuario dado
     */
    boolean existsByUsername(String username);

    /**
     * Verifica si existe un usuario con el email dado
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si existe un usuario con el ID dado
     */
    boolean existsById(String id);

    /**
     * Elimina un usuario por ID (soft delete)
     */
    void deleteById(String id);

    /**
     * Elimina permanentemente un usuario (hard delete)
     */
    void hardDeleteById(String id);

    /**
     * Cuenta los usuarios visibles
     */
    long count();
}