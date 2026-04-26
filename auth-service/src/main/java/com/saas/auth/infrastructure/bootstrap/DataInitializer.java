package com.saas.auth.infrastructure.bootstrap;

import com.saas.auth.domain.model.User;
import com.saas.auth.domain.port.in.IUserUseCase;
import com.saas.auth.domain.port.out.IUserRepositoryPort;
import com.saas.auth.domain.port.out.IUserRoleRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Crea el usuario administrador de bootstrap si no existe.
 * Idempotente: ejecucion segura en cada arranque.
 *
 * El password viene en {@code saas.bootstrap.admin.password} y se cifra con BCrypt
 * en runtime (no se puede precalcular en SQL).
 *
 * Asigna automaticamente el rol ADMIN (Id deterministico desde V2__seed_base.sql).
 */
@Slf4j
@Component
public class DataInitializer {

    /** Id deterministico del rol ADMIN en V2__seed_base.sql */
    private static final UUID ADMIN_ROLE_ID =
            UUID.fromString("11111111-0000-0000-0000-000000000001");

    private final IUserUseCase userUseCase;
    private final IUserRepositoryPort userRepo;
    private final IUserRoleRepositoryPort userRoleRepo;

    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFirstName;
    private final String adminLastName;

    public DataInitializer(IUserUseCase userUseCase,
                            IUserRepositoryPort userRepo,
                            IUserRoleRepositoryPort userRoleRepo,
                            @Value("${saas.bootstrap.admin.username:admin}") String adminUsername,
                            @Value("${saas.bootstrap.admin.email:admin@saas.local}") String adminEmail,
                            @Value("${saas.bootstrap.admin.password:Admin123!}") String adminPassword,
                            @Value("${saas.bootstrap.admin.first-name:Administrador}") String adminFirstName,
                            @Value("${saas.bootstrap.admin.last-name:Sistema}") String adminLastName) {
        this.userUseCase = userUseCase;
        this.userRepo = userRepo;
        this.userRoleRepo = userRoleRepo;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFirstName = adminFirstName;
        this.adminLastName = adminLastName;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrapAdminUser() {
        if (userRepo.existsByUsername(adminUsername)) {
            log.info("Admin user '{}' ya existe; bootstrap omitido.", adminUsername);
            return;
        }

        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .firstName(adminFirstName)
                .lastName(adminLastName)
                .theme("light")
                .languageCode("es-CO")
                .build();

        User created = userUseCase.createWithPassword(admin, adminPassword);
        userRoleRepo.replaceRolesForUser(created.getId(), Set.of(ADMIN_ROLE_ID));

        log.info("Admin user creado: {} ({}). Cambia el password tras el primer login.",
                adminUsername, created.getId());
    }
}
