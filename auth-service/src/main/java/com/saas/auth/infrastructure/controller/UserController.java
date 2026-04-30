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
import com.saas.common.security.IUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserUseCase userUseCase;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal IUserPrincipal principal) {
        User user = userUseCase.loadWithRoles(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(toResponseWithRoles(user)));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal IUserPrincipal principal,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        userUseCase.changePassword(principal.getUserId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password actualizado"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        // getAll() no carga roleCodes; hidratamos por usuario antes de mapear.
        // Endpoint admin-only y N suele ser pequeno: el N+1 es aceptable.
        // Si crece a miles, mover a un loadAllWithRoles batch (1 query user_role + 1 Feign).
        List<UserResponse> users = userUseCase.getAll().stream()
                .map(u -> userUseCase.loadWithRoles(u.getId()))
                .map(this::toResponseWithRoles)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable UUID id) {
        User user = userUseCase.loadWithRoles(id);
        return ResponseEntity.ok(ApiResponse.success(toResponseWithRoles(user)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        User domain = userMapper.toDomain(request);
        User created = userUseCase.createWithPassword(domain, request.password());
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            userUseCase.assignRoles(created.getId(), request.roleIds());
            created = userUseCase.loadWithRoles(created.getId());
        }
        return ResponseEntity.ok(ApiResponse.created(toResponseWithRoles(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable UUID id,
                                                              @Valid @RequestBody UpdateUserRequest request) {
        User existing = userUseCase.getById(id);
        userMapper.updateDomainFromRequest(request, existing);
        User updated = userUseCase.update(id, existing);
        return ResponseEntity.ok(ApiResponse.success(toResponseWithRoles(userUseCase.loadWithRoles(updated.getId()))));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignRoles(@PathVariable UUID id,
                                                           @Valid @RequestBody AssignRolesRequest request) {
        userUseCase.assignRoles(id, request.roleIds());
        return ResponseEntity.ok(ApiResponse.success(null, "Roles asignados"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Usuario deshabilitado"));
    }

    private UserResponse toResponseWithRoles(User user) {
        UserResponse base = userMapper.toResponse(user);
        return new UserResponse(
                base.id(), base.username(), base.email(), base.firstName(), base.lastName(),
                base.fullName(), base.profilePhoto(), base.theme(), base.languageCode(),
                base.lastLoginAt(), base.enabled(), base.visible(),
                user.getRoleCodes(), base.createdDate()
        );
    }
}
