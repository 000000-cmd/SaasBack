package com.saas.systemservice.infrastructure.adapters.out.persistence;

import com.saas.systemservice.domain.model.menu.RoleMenu;
import com.saas.systemservice.domain.ports.out.menu.IRoleMenuRepositoryPort;
import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.menu.RoleMenuEntity;
import com.saas.systemservice.infrastructure.adapters.out.persistence.mapper.menu.RoleMenuPersistenceMapper;
import com.saas.systemservice.infrastructure.adapters.out.persistence.repository.menu.JpaRoleMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DatabaseRoleMenuAdapter implements IRoleMenuRepositoryPort {

    private final JpaRoleMenuRepository jpaRepository;
    private final RoleMenuPersistenceMapper mapper;

    @Override
    public RoleMenu save(RoleMenu domain) {
        RoleMenuEntity entity = mapper.toEntity(domain);
        RoleMenuEntity saved = jpaRepository.save(entity);
        RoleMenu result = mapper.toDomain(saved);

        if (result.getRoleCode() == null) {
            result.setRoleCode(domain.getRoleCode());
        }
        if (result.getMenuCode() == null) {
            result.setMenuCode(domain.getMenuCode());
        }
        return result;
    }

    @Override
    public void deleteById(String id) {
        if (id != null) jpaRepository.deleteById(UUID.fromString(id));
    }

    @Override
    public RoleMenu findById(String id) {
        return jpaRepository.findById(UUID.fromString(id))
                .map(mapper::toDomain).orElse(null);
    }

    @Override
    public List<RoleMenu> findByRoleCode(String roleCode) {
        return jpaRepository.findByRole_Code(roleCode).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByRoleAndMenu(String roleId, String menuId) {
        if (roleId == null || menuId == null) return false;
        return jpaRepository.existsByRole_IdAndMenu_Id(UUID.fromString(roleId), UUID.fromString(menuId));
    }
}
