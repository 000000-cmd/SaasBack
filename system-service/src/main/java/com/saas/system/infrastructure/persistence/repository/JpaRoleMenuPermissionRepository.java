package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.RoleMenuPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para RoleMenuPermissionEntity.
 */
@Repository
public interface JpaRoleMenuPermissionRepository extends JpaRepository<RoleMenuPermissionEntity, UUID> {

    List<RoleMenuPermissionEntity> findByRoleMenuId(UUID roleMenuId);

    boolean existsByRoleMenuIdAndPermissionId(UUID roleMenuId, UUID permissionId);

    @Modifying
    @Query("DELETE FROM RoleMenuPermissionEntity rmp WHERE rmp.roleMenu.id = :roleMenuId")
    void deleteByRoleMenuId(@Param("roleMenuId") UUID roleMenuId);

    @Modifying
    @Query("DELETE FROM RoleMenuPermissionEntity rmp WHERE rmp.roleMenu.id = :roleMenuId AND rmp.permission.id = :permissionId")
    void deleteByRoleMenuIdAndPermissionId(@Param("roleMenuId") UUID roleMenuId, @Param("permissionId") UUID permissionId);
}
