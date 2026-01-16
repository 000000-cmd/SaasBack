package com.saas.systemservice.infrastructure.adapters.out.persistence.repository.lists;

import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.lists.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaRoleRepository extends JpaRepository<RoleEntity, UUID> {
    Optional<RoleEntity> findByCode(String code);
    boolean existsByCode(String code);
}