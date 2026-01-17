package com.saas.system.infrastructure.persistence.adapter;

import com.saas.system.domain.model.Role;
import com.saas.system.domain.port.out.IRoleRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import com.saas.system.infrastructure.persistence.mapper.RolePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Roles.
 */
@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements IRoleRepositoryPort {

    private final JpaRoleRepository jpaRepository;
    private final RolePersistenceMapper mapper;

    @Override
    public Role save(Role entity) {
        RoleEntity jpaEntity = mapper.toEntity(entity);
        RoleEntity saved = jpaRepository.save(jpaEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public Role update(Role entity) {
        return save(entity);
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findByVisibleTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Role> findAllIncludingHidden() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Role> findById(String id) {
        if (id == null) return Optional.empty();
        return jpaRepository.findById(UUID.fromString(id))
                .filter(RoleEntity::getVisible)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .filter(RoleEntity::getVisible)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(String id) {
        if (id != null) {
            jpaRepository.findById(UUID.fromString(id)).ifPresent(entity -> {
                entity.setVisible(false);
                entity.setEnabled(false);
                jpaRepository.save(entity);
            });
        }
    }

    @Override
    public void hardDeleteById(String id) {
        if (id != null) {
            jpaRepository.deleteById(UUID.fromString(id));
        }
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public boolean existsById(String id) {
        if (id == null) return false;
        return jpaRepository.existsById(UUID.fromString(id));
    }

    @Override
    public long count() {
        return jpaRepository.findByVisibleTrue().size();
    }
}
