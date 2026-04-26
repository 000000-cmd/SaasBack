package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.MenuRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaMenuRoleRepository extends JpaRepository<MenuRoleEntity, UUID> {

    @Query("SELECT mr FROM MenuRoleEntity mr WHERE mr.menu.id = :menuId")
    List<MenuRoleEntity> findByMenuId(@Param("menuId") UUID menuId);

    @Query("SELECT mr FROM MenuRoleEntity mr WHERE mr.role.id = :roleId")
    List<MenuRoleEntity> findByRoleId(@Param("roleId") UUID roleId);

    @Query("SELECT (COUNT(mr) > 0) FROM MenuRoleEntity mr WHERE mr.menu.id = :menuId AND mr.role.id = :roleId")
    boolean existsByMenuIdAndRoleId(@Param("menuId") UUID menuId, @Param("roleId") UUID roleId);
}
