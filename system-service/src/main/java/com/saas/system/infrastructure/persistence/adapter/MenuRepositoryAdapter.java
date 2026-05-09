package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.Menu;
import com.saas.system.domain.port.out.IMenuRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import com.saas.system.infrastructure.persistence.mapper.MenuPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaMenuRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class MenuRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Menu, MenuEntity, UUID>
        implements IMenuRepositoryPort {

    private final JpaMenuRepository jpa;
    private final MenuPersistenceMapper menuMapper;

    public MenuRepositoryAdapter(JpaMenuRepository jpa, MenuPersistenceMapper mapper) {
        super(jpa, mapper, "Menu");
        this.jpa = jpa;
        this.menuMapper = mapper;
    }

    /**
     * Override de update para permitir cambiar el {@code parent} (jerarquia).
     * El base no toca el parent porque updateEntityFromDomain ignora referencias.
     */
    @Override
    @Transactional
    public Menu update(Menu domain) {
        MenuEntity existing = jpa.findById(domain.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu", "Id", domain.getId()));
        menuMapper.updateEntityFromDomain(domain, existing);
        existing.setParent(menuMapper.toEntity(domain).getParent());
        return menuMapper.toDomain(jpa.save(existing));
    }

    @Override
    public Optional<Menu> findByCode(String code) {
        return jpa.findByCode(code).map(menuMapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    @Override
    public List<Menu> findRootMenus() {
        return menuMapper.toDomainList(jpa.findRootMenus());
    }

    @Override
    public List<Menu> findByParentId(UUID parentId) {
        return menuMapper.toDomainList(jpa.findByParentId(parentId));
    }

    @Override
    public List<Menu> findByRoleIds(Set<UUID> roleIds) {
        return roleIds == null || roleIds.isEmpty()
                ? List.of()
                : menuMapper.toDomainList(jpa.findByRoleIds(roleIds));
    }
}
