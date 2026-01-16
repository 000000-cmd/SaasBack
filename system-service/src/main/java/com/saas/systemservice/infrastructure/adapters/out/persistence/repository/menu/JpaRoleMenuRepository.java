package com.saas.systemservice.infrastructure.adapters.out.persistence.repository.menu;

import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.menu.RoleMenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaRoleMenuRepository extends JpaRepository<RoleMenuEntity, UUID> {

    boolean existsByRole_IdAndMenu_Id(UUID roleId, UUID menuId);
    List<RoleMenuEntity> findByRole_Code(String roleCode);
}
