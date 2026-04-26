package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.Menu;
import com.saas.system.domain.port.out.IMenuRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import com.saas.system.infrastructure.persistence.mapper.MenuPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MenuRepositoryAdapter implements IMenuRepositoryPort {

    private final JpaMenuRepository jpa;
    private final MenuPersistenceMapper mapper;

    @Override
    public Menu save(Menu domain) {
        return mapper.toDomain(jpa.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional
    public Menu update(Menu domain) {
        MenuEntity existing = jpa.findById(domain.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu", "Id", domain.getId()));
        mapper.updateEntityFromDomain(domain, existing);
        // El parent SI debe poder cambiar (permite reorganizar la jerarquia)
        existing.setParent(mapper.toEntity(domain).getParent());
        return mapper.toDomain(jpa.save(existing));
    }

    @Override public Optional<Menu> findById(UUID id)            { return jpa.findById(id).map(mapper::toDomain); }
    @Override public boolean        existsById(UUID id)          { return jpa.existsById(id); }
    @Override public List<Menu>     findAll()                     { return mapper.toDomainList(jpa.findAll()); }
    @Override public Optional<Menu> findByCode(String code)      { return jpa.findByCode(code).map(mapper::toDomain); }
    @Override public boolean        existsByCode(String code)    { return jpa.existsByCode(code); }
    @Override public List<Menu>     findRootMenus()               { return mapper.toDomainList(jpa.findRootMenus()); }
    @Override public List<Menu>     findByParentId(UUID parentId) { return mapper.toDomainList(jpa.findByParentId(parentId)); }
    @Override public List<Menu>     findByRoleIds(Set<UUID> roleIds) {
        return roleIds == null || roleIds.isEmpty() ? List.of() : mapper.toDomainList(jpa.findByRoleIds(roleIds));
    }

    @Override
    @Transactional
    public void softDeleteById(UUID id) {
        jpa.findById(id).ifPresent(e -> {
            e.setEnabled(false);
            e.setVisible(false);
            jpa.save(e);
        });
    }

    @Override public void hardDeleteById(UUID id) { jpa.deleteById(id); }
}
