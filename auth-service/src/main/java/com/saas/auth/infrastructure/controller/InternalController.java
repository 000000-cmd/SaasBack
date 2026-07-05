package com.saas.auth.infrastructure.controller;

import com.saas.auth.application.dto.event.UserEventPayload;
import com.saas.auth.domain.model.User;
import com.saas.auth.domain.port.in.IUserUseCase;
import com.saas.common.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalController {

    /**
     * Roles sembrados con id fijo en V1. Los consumidores S2S piden por CODIGO
     * (regla del proyecto: condicionales por codigo) y aqui se resuelve el id.
     */
    private static final Map<String, UUID> SEEDED_ROLES = Map.of(
            "OWNER", UUID.fromString("11111111-0000-0000-0000-000000000004"),
            "EMPLOYEE", UUID.fromString("11111111-0000-0000-0000-000000000005"));

    private final IUserUseCase userUseCase;

    /** Alta S2S de una cuenta (p.ej. business-service crea la cuenta del empleado). */
    @PostMapping("/users")
    public CreatedUser createUser(@Valid @RequestBody CreateInternalUserRequest req) {
        Set<UUID> roleIds = req.roleCodes().stream()
                .map(code -> {
                    UUID id = SEEDED_ROLES.get(code);
                    if (id == null) throw new BusinessException("Rol no soportado para alta interna: " + code);
                    return id;
                })
                .collect(Collectors.toSet());

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .firstName(req.firstName())
                .lastName(req.lastName())
                .build();
        User created = userUseCase.createWithPassword(user, req.password());
        userUseCase.assignRoles(created.getId(), roleIds);
        log.info("Alta interna de usuario: id={} username={} roles={}", created.getId(), created.getUsername(), req.roleCodes());
        return new CreatedUser(created.getId(), created.getUsername(), created.getEmail());
    }

    public record CreateInternalUserRequest(
            @NotBlank @Size(max = 60) String username,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Size(min = 8, max = 60) String password,
            @NotEmpty Set<String> roleCodes
    ) {}

    public record CreatedUser(UUID id, String username, String email) {}

    @GetMapping("/users/all")
    public List<UserEventPayload> listAllForReindex(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        log.info("Reindex fetch users: page={} size={}", page, size);
        // findAllPaged NO carga roleCodes; hidratamos por user con loadWithRoles.
        // Para 1000s de users esto hace 1 llamada Feign por user al system-service,
        // pero el cache Caffeine del RoleResolverFeignAdapter mitiga (5 min TTL).
        // Si crece a millones, optimizar con bulk fetch + un solo Feign call batch.
        return userUseCase.findAllPaged(page, size).stream()
                .map(u -> userUseCase.loadWithRoles(u.getId()))
                .map(UserEventPayload::from)
                .toList();
    }
    @GetMapping("/users/count")
    public Map<String, Long> countUsers() {
        return Map.of("total", userUseCase.count());
    }


}
