package com.saas.auth.application.service;

import com.saas.auth.domain.model.User;
import com.saas.auth.domain.model.UserRole;
import com.saas.auth.domain.port.in.IUserUseCase;
import com.saas.auth.domain.port.out.IRoleResolverPort;
import com.saas.auth.domain.port.out.IUserRepositoryPort;
import com.saas.auth.domain.port.out.IUserRoleRepositoryPort;
import com.saas.common.exception.BusinessException;
import com.saas.common.exception.DuplicateResourceException;
import com.saas.common.exception.InvalidCredentialsException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.service.GenericCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService extends GenericCrudService<User, UUID> implements IUserUseCase {

    private final IUserRepositoryPort userRepo;
    private final IUserRoleRepositoryPort userRoleRepo;
    private final IRoleResolverPort roleResolver;
    private final PasswordEncoder passwordEncoder;

    public UserService(IUserRepositoryPort userRepo,
                        IUserRoleRepositoryPort userRoleRepo,
                        IRoleResolverPort roleResolver,
                        PasswordEncoder passwordEncoder) {
        super(userRepo);
        this.userRepo = userRepo;
        this.userRoleRepo = userRoleRepo;
        this.roleResolver = roleResolver;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected String getResourceName() { return "Usuario"; }

    @Override
    protected void applyChanges(User existing, User incoming) {
        if (incoming.getUsername() != null)     existing.setUsername(incoming.getUsername());
        if (incoming.getEmail() != null)        existing.setEmail(incoming.getEmail());
        if (incoming.getFirstName() != null)    existing.setFirstName(incoming.getFirstName());
        if (incoming.getLastName() != null)     existing.setLastName(incoming.getLastName());
        if (incoming.getProfilePhoto() != null) existing.setProfilePhoto(incoming.getProfilePhoto());
        if (incoming.getTheme() != null)        existing.setTheme(incoming.getTheme());
        if (incoming.getLanguageCode() != null) existing.setLanguageCode(incoming.getLanguageCode());
    }

    @Override
    protected void onBeforeCreate(User user) {
        if (userRepo.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Usuario", "username", user.getUsername());
        }
        if (userRepo.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Usuario", "email", user.getEmail());
        }
    }

    @Override
    protected void onBeforeUpdate(User existing, User incoming) {
        if (incoming.getUsername() != null
                && !incoming.getUsername().equals(existing.getUsername())
                && userRepo.existsByUsername(incoming.getUsername())) {
            throw new DuplicateResourceException("Usuario", "username", incoming.getUsername());
        }
        if (incoming.getEmail() != null
                && !incoming.getEmail().equals(existing.getEmail())
                && userRepo.existsByEmail(incoming.getEmail())) {
            throw new DuplicateResourceException("Usuario", "email", incoming.getEmail());
        }
    }

    @Override
    @Transactional
    public User createWithPassword(User user, String rawPassword) {
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        if (user.getTheme() == null)        user.setTheme("light");
        if (user.getLanguageCode() == null) user.setLanguageCode("es-CO");
        return create(user);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = getById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Password actual incorrecto");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException("El nuevo password debe ser distinto al actual");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.update(user);
        log.info("Password cambiado: userId={}", userId);
    }

    @Override
    @Transactional
    public void assignRoles(UUID userId, Set<UUID> roleIds) {
        if (!userRepo.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario", "Id", userId);
        }
        userRoleRepo.replaceRolesForUser(userId, roleIds);
    }

    @Override
    public Optional<User> getByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public User loadWithRoles(UUID userId) {
        User user = getById(userId);
        Set<UUID> roleIds = userRoleRepo.findByUserId(userId).stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());
        Set<String> codes = roleIds.isEmpty()
                ? Set.of()
                : new HashSet<>(roleResolver.resolveCodes(roleIds));
        user.setRoleCodes(codes);
        return user;
    }
}
