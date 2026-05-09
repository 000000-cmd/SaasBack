package com.saas.auth.infrastructure.persistence.adapter;

import com.saas.auth.domain.model.UserRole;
import com.saas.auth.domain.port.out.IUserRoleRepositoryPort;
import com.saas.auth.infrastructure.persistence.entity.UserEntity;
import com.saas.auth.infrastructure.persistence.entity.UserRoleEntity;
import com.saas.auth.infrastructure.persistence.mapper.UserRolePersistenceMapper;
import com.saas.auth.infrastructure.persistence.repository.JpaUserRoleRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class UserRoleRepositoryAdapter
        extends BaseJpaRepositoryAdapter<UserRole, UserRoleEntity, UUID>
        implements IUserRoleRepositoryPort {

    private final JpaUserRoleRepository jpa;

    public UserRoleRepositoryAdapter(JpaUserRoleRepository jpa,
                                     UserRolePersistenceMapper mapper) {
        super(jpa, mapper, "UserRole");
        this.jpa = jpa;
    }

    @Override
    public List<UserRole> findByUserId(UUID userId) {
        return getMapper().toDomainList(jpa.findByUserId(userId));
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
