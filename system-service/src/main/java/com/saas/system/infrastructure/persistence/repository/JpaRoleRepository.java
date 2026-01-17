package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para RoleEntity.
 */
@Repository
public interface JpaRoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<RoleEntity> findByVisibleTrue();
}