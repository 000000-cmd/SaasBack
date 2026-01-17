package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.RoleMenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para RoleMenuEntity.
 */
@Repository
public interface JpaRoleMenuRepository extends JpaRepository<RoleMenuEntity, UUID> {

    List<RoleMenuEntity> findByRoleId(UUID roleId);

    List<RoleMenuEntity> findByRoleCode(String roleCode);

    List<RoleMenuEntity> findByMenuId(UUID menuId);

    boolean existsByRoleIdAndMenuId(UUID roleId, UUID menuId);

    @Modifying
    @Query("DELETE FROM RoleMenuEntity rm WHERE rm.role.id = :roleId AND rm.menu.id = :menuId")
    void deleteByRoleIdAndMenuId(@Param("roleId") UUID roleId, @Param("menuId") UUID menuId);
}
