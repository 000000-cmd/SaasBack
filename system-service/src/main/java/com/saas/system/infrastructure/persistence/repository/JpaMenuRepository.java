package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para MenuEntity.
 */
@Repository
public interface JpaMenuRepository extends JpaRepository<MenuEntity, UUID> {

    Optional<MenuEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<MenuEntity> findByVisibleTrue();

    List<MenuEntity> findByParentIdAndVisibleTrue(UUID parentId);

    List<MenuEntity> findByParentIdIsNullAndVisibleTrue();
}
