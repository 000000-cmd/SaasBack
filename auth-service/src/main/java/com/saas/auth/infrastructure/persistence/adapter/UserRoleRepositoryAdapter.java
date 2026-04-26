package com.saas.auth.infrastructure.persistence.adapter;

import com.saas.auth.domain.model.UserRole;
import com.saas.auth.domain.port.out.IUserRoleRepositoryPort;
import com.saas.auth.infrastructure.persistence.entity.UserEntity;
import com.saas.auth.infrastructure.persistence.entity.UserRoleEntity;
import com.saas.auth.infrastructure.persistence.mapper.UserRolePersistenceMapper;
import com.saas.auth.infrastructure.persistence.repository.JpaUserRoleRepository;
import com.saas.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRoleRepositoryAdapter implements IUserRoleRepositoryPort {

    private final JpaUserRoleRepository jpa;
    private final UserRolePersistenceMapper mapper;

    @Override
    public UserRole save(UserRole entity) {
        return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
    }

    @Override
    @Transactional
    public UserRole update(UserRole entity) {
        UserRoleEntity existing = jpa.findById(entity.getId())
                .orElseThrow(() -> new ResourceNotFoundException("UserRole", "Id", entity.getId()));
        mapper.updateEntityFromDomain(entity, existing);
        return mapper.toDomain(jpa.save(existing));
    }

    @Override
    public Optional<UserRole> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public List<UserRole> findAll() {
        return mapper.toDomainList(jpa.findAll());
    }

    @Override
    @Transactional
    public void softDeleteById(UUID id) {
        jpa.findById(id).ifPresent(e -> {
            e.setEnabled(false);
            e.setVisible(false);
            jpa.save(e);
        });
    }

    @Override
    public void hardDeleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public List<UserRole> findByUserId(UUID userId) {
        return mapper.toDomainList(jpa.findByUserId(userId));
    }

    @Override
    @Transactional
    public void replaceRolesForUser(UUID userId, Set<UUID> roleIds) {
        Set<UUID> desired = roleIds == null ? Set.of() : new HashSet<>(roleIds);
        List<UserRoleEntity> current = jpa.findByUserId(userId);
        Set<UUID> currentIds = current.stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toSet());

        // Borrar los que ya no estan
        current.stream()
                .filter(ur -> !desired.contains(ur.getRoleId()))
                .forEach(jpa::delete);

        // Agregar los nuevos
        UserEntity userRef = new UserEntity();
        userRef.setId(userId);
        desired.stream()
                .filter(rid -> !currentIds.contains(rid))
                .map(rid -> UserRoleEntity.builder()
                        .user(userRef)
                        .roleId(rid)
                        .build())
                .forEach(jpa::save);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpa.deleteByUserId(userId);
    }

    @Override
    public boolean existsByUserIdAndRoleId(UUID userId, UUID roleId) {
        return jpa.existsByUserIdAndRoleId(userId, roleId);
    }
}
