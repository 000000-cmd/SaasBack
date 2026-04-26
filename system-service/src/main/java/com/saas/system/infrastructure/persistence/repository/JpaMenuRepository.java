package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface JpaMenuRepository extends JpaRepository<MenuEntity, UUID> {

    Optional<MenuEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT m FROM MenuEntity m WHERE m.parent IS NULL ORDER BY m.displayOrder")
    List<MenuEntity> findRootMenus();

    @Query("SELECT m FROM MenuEntity m WHERE m.parent.id = :parentId ORDER BY m.displayOrder")
    List<MenuEntity> findByParentId(@Param("parentId") UUID parentId);

    @Query("""
            SELECT DISTINCT m FROM MenuEntity m
            JOIN MenuRoleEntity mr ON mr.menu.id = m.id
            WHERE mr.role.id IN :roleIds
              AND m.enabled = true
              AND m.visible = true
              AND mr.enabled = true
            ORDER BY m.displayOrder
            """)
    List<MenuEntity> findByRoleIds(@Param("roleIds") Set<UUID> roleIds);
}
