package com.saas.auth.application.service;

import com.saas.auth.domain.model.User;
import com.saas.auth.domain.port.in.IUserUseCase;
import com.saas.auth.domain.port.out.IUserRepositoryPort;
import com.saas.common.exception.BusinessException;
import com.saas.common.exception.DuplicateResourceException;
import com.saas.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de aplicación para gestión de usuarios.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserUseCase {

    private final IUserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User create(User user, String rawPassword) {
        log.debug("Creando usuario: {}", user.getUsername());

        // Validar duplicados
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Usuario", "username", user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Usuario", "email", user.getEmail());
        }

        // Generar ID y encriptar contraseña
        user.setId(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.markAsCreated("SYSTEM");

        // Asegurar que el conjunto de roles esté inicializado
        if (user.getRoleCodes() == null) {
            user.setRoleCodes(new HashSet<>());
        }

        User saved = userRepository.save(user);
        log.info("Usuario creado con ID: {}", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public User update(String id, User user) {
        log.debug("Actualizando usuario: {}", id);

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "ID", id));

        // Validar email único si cambió
        if (!existing.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Usuario", "email", user.getEmail());
        }

        // Actualizar campos permitidos
        existing.setEmail(user.getEmail());
        existing.setCellular(user.getCellular());
        existing.setAttachment(user.getAttachment());
        existing.markAsUpdated("SYSTEM");

        User updated = userRepository.update(existing);
        log.info("Usuario actualizado: {}", id);

        return updated;
    }

    @Override
    @Transactional
    public void changePassword(String id, String currentPassword, String newPassword) {
        log.debug("Cambiando contraseña para usuario: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "ID", id));

        // Verificar contraseña actual
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("La contraseña actual es incorrecta");
        }

        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(newPassword));
        user.markAsUpdated("SYSTEM");

        userRepository.update(user);
        log.info("Contraseña cambiada para usuario: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "ID", id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "username", username));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.debug("Eliminando usuario: {}", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuario", "ID", id);
        }

        userRepository.deleteById(id);
        log.info("Usuario eliminado: {}", id);
    }

    @Override
    @Transactional
    public void toggleEnabled(String id, boolean enabled) {
        log.debug("Cambiando estado de usuario {} a: {}", id, enabled);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "ID", id));

        user.setEnabled(enabled);
        user.markAsUpdated("SYSTEM");

        userRepository.update(user);
        log.info("Estado de usuario {} cambiado a: {}", id, enabled);
    }

    @Override
    @Transactional
    public void assignRoles(String userId, List<String> roleCodes) {
        log.debug("Asignando roles a usuario {}: {}", userId, roleCodes);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "ID", userId));

        // Reemplazar roles existentes
        user.setRoleCodes(new HashSet<>(roleCodes));
        user.markAsUpdated("SYSTEM");

        userRepository.update(user);
        log.info("Roles asignados a usuario {}: {}", userId, roleCodes);
    }

    @Override
    @Transactional
    public void removeRole(String userId, String roleCode) {
        log.debug("Removiendo rol {} de usuario {}", roleCode, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "ID", userId));

        user.removeRole(roleCode);
        user.markAsUpdated("SYSTEM");

        userRepository.update(user);
        log.info("Rol {} removido de usuario {}", roleCode, userId);
    }
}