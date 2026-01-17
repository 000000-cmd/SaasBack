package com.saas.auth.infrastructure.controller;

import com.saas.auth.application.dto.request.AssignRolesRequest;
import com.saas.auth.application.dto.request.ChangePasswordRequest;
import com.saas.auth.application.dto.request.CreateUserRequest;
import com.saas.auth.application.dto.request.UpdateUserRequest;
import com.saas.auth.application.dto.response.UserResponse;
import com.saas.auth.application.mapper.UserMapper;
import com.saas.auth.domain.model.User;
import com.saas.auth.domain.port.in.IUserUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;

/**
 * Controlador REST para gestión de usuarios.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserUseCase userUseCase;
    private final UserMapper userMapper;

    /**
     * Crea un nuevo usuario.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody CreateUserRequest request) {

        log.debug("Creando usuario: {}", request.getUsername());

        User user = userMapper.toDomain(request);
        if (request.getRoleCodes() != null) {
            user.setRoleCodes(new HashSet<>(request.getRoleCodes()));
        }

        User created = userUseCase.create(user, request.getPassword());
        UserResponse response = userMapper.toResponse(created);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Usuario creado exitosamente"));
    }

    /**
     * Obtiene todos los usuarios.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        List<User> users = userUseCase.getAll();
        List<UserResponse> response = userMapper.toResponseList(users);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Obtiene un usuario por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable String id) {
        User user = userUseCase.getById(id);
        UserResponse response = userMapper.toResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Obtiene el usuario actual autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("No autenticado", 401));
        }

        User user = userUseCase.getByUsername(userDetails.getUsername());
        UserResponse response = userMapper.toResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Actualiza un usuario.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {

        log.debug("Actualizando usuario: {}", id);

        User user = userMapper.toDomain(request);
        User updated = userUseCase.update(id, user);
        UserResponse response = userMapper.toResponse(updated);

        return ResponseEntity.ok(ApiResponse.success(response, "Usuario actualizado exitosamente"));
    }

    /**
     * Cambia la contraseña de un usuario.
     */
    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable String id,
            @Valid @RequestBody ChangePasswordRequest request) {

        log.debug("Cambiando contraseña para usuario: {}", id);

        userUseCase.changePassword(id, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success(null, "Contraseña actualizada exitosamente"));
    }

    /**
     * Activa o desactiva un usuario.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(
            @PathVariable String id,
            @RequestParam boolean enabled) {

        userUseCase.toggleEnabled(id, enabled);

        return ResponseEntity.ok(ApiResponse.success(null,
                enabled ? "Usuario habilitado" : "Usuario deshabilitado"));
    }

    /**
     * Asigna roles a un usuario.
     */
    @PostMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<Void>> assignRoles(
            @PathVariable String id,
            @Valid @RequestBody AssignRolesRequest request) {

        log.debug("Asignando roles a usuario {}: {}", id, request.getRoleCodes());

        userUseCase.assignRoles(id, request.getRoleCodes());

        return ResponseEntity.ok(ApiResponse.success(null, "Roles asignados exitosamente"));
    }

    /**
     * Remueve un rol de un usuario.
     */
    @DeleteMapping("/{id}/roles/{roleCode}")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable String id,
            @PathVariable String roleCode) {

        log.debug("Removiendo rol {} de usuario {}", roleCode, id);

        userUseCase.removeRole(id, roleCode);

        return ResponseEntity.ok(ApiResponse.success(null, "Rol removido exitosamente"));
    }

    /**
     * Elimina un usuario.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        log.debug("Eliminando usuario: {}", id);

        userUseCase.delete(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Usuario eliminado exitosamente"));
    }
}
