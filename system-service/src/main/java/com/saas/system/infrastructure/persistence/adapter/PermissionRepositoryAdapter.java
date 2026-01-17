package com.saas.system.infrastructure.persistence.adapter;

import com.saas.system.domain.model.Permission;
import com.saas.system.domain.port.out.IPermissionRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.PermissionEntity;
import com.saas.system.infrastructure.persistence.mapper.PermissionPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Permisos.
 */
@Repository
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements IPermissionRepositoryPort {

    private final JpaPermissionRepository jpaRepository;
    private final PermissionPersistenceMapper mapper;

    @Override
    public Permission save(Permission entity) {
        PermissionEntity jpaEntity = mapper.toEntity(entity);
        PermissionEntity saved = jpaRepository.save(jpaEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public Permission update(Permission entity) {
        return save(entity);
    }

    @Override
    public List<Permission> findAll() {
        return jpaRepository.findByVisibleTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Permission> findAllIncludingHidden() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Permission> findById(String id) {
        if (id == null) return Optional.empty();
        return jpaRepository.findById(UUID.fromString(id))
                .filter(PermissionEntity::getVisible)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Permission> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .filter(PermissionEntity::getVisible)
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