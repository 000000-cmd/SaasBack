package com.saas.auth.domain.port.in;

import com.saas.auth.domain.model.User;

import java.util.List;

/**
 * Puerto de entrada para casos de uso de gestión de usuarios.
 */
public interface IUserUseCase {

    /**
     * Crea un nuevo usuario.
     *
     * @param user Usuario a crear
     * @param rawPassword Contraseña en texto plano (se encriptará)
     * @return Usuario creado
     */
    User create(User user, String rawPassword);

    /**
     * Actualiza un usuario existente.
     *
     * @param id ID del usuario
     * @param user Datos actualizados
     * @return Usuario actualizado
     */
    User update(String id, User user);

    /**
     * Cambia la contraseña de un usuario.
     *
     * @param id ID del usuario
     * @param currentPassword Contraseña actual
     * @param newPassword Nueva contraseña
     */
    void changePassword(String id, String currentPassword, String newPassword);

    /**
     * Obtiene un usuario por su ID.
     *
     * @param id ID del usuario
     * @return Usuario encontrado
     */
    User getById(String id);

    /**
     * Obtiene un usuario por su username.
     *
     * @param username Nombre de usuario
     * @return Usuario encontrado
     */
    User getByUsername(String username);

    /**
     * Obtiene un usuario por su email.
     *
     * @param email Email del usuario
     * @return Usuario encontrado
     */
    User getByEmail(String email);

    /**
     * Obtiene todos los usuarios.
     *
     * @return Lista de usuarios
     */
    List<User> getAll();

    /**
     * Elimina un usuario (soft delete).
     *
     * @param id ID del usuario
     */
    void delete(String id);

    /**
     * Activa o desactiva un usuario.
     *
     * @param id ID del usuario
     * @param enabled Estado
     */
    void toggleEnabled(String id, boolean enabled);

    /**
     * Asigna roles a un usuario.
     *
     * @param userId ID del usuario
     * @param roleCodes Códigos de roles a asignar
     */
    void assignRoles(String userId, List<String> roleCodes);

    /**
     * Remueve un rol de un usuario.
     *
     * @param userId ID del usuario
     * @param roleCode Código del rol a remover
     */
    void removeRole(String userId, String roleCode);
}