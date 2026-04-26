package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface JpaRolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {

    @Query("SELECT rp FROM RolePermissionEntity rp WHERE rp.role.id = :roleId")
    List<RolePermissionEntity> findByRoleId(@Param("roleId") UUID roleId);

    @Query("""
            SELECT rp.permission.code FROM RolePermissionEntity rp
            WHERE rp.role.id = :roleId
              AND rp.enabled = true
              AND rp.permission.enabled = true
            """)
    Set<String> findPermissionCodesByRoleId(@Param("roleId") UUID roleId);

    @Query("""
            SELECT (COUNT(rp) > 0) FROM RolePermissionEntity rp
            WHERE rp.role.id = :roleId AND rp.permission.id = :permissionId
            """)
    boolean existsByRoleIdAndPermissionId(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);
}
