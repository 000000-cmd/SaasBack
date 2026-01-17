package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para PermissionEntity.
 */
@Repository
public interface JpaPermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    Optional<PermissionEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<PermissionEntity> findByVisibleTrue();
}