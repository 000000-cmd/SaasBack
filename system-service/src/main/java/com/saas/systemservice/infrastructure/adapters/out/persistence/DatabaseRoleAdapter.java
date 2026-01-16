package com.saas.systemservice.infrastructure.adapters.out.persistence;

import com.saas.systemservice.domain.model.lists.Role;
import com.saas.systemservice.domain.ports.out.lists.IRoleRepositoryPort;
import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.lists.RoleEntity;
import com.saas.systemservice.infrastructure.adapters.out.persistence.mapper.RolePersistenceMapper;
import com.saas.systemservice.infrastructure.adapters.out.persistence.repository.lists.JpaRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DatabaseRoleAdapter implements IRoleRepositoryPort {

    private final JpaRoleRepository jpaRepository;
    private final RolePersistenceMapper mapper;

    @Override
    public Role save(Role role) {
        RoleEntity entity = mapper.toEntity(role);
        RoleEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Role update(Role role) {
        return save(role); // JPA usa save para create y update
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Role> findById(String id) {
        if (id == null) return Optional.empty();
        return jpaRepository.findById(UUID.fromString(id))
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(String id) {
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
}
