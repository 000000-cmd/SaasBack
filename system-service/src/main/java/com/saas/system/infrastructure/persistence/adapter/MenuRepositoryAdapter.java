package com.saas.system.infrastructure.persistence.adapter;

import com.saas.system.domain.model.Menu;
import com.saas.system.domain.port.out.IMenuRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import com.saas.system.infrastructure.persistence.mapper.MenuPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Menús.
 */
@Repository
@RequiredArgsConstructor
public class MenuRepositoryAdapter implements IMenuRepositoryPort {

    private final JpaMenuRepository jpaRepository;
    private final MenuPersistenceMapper mapper;

    @Override
    public Menu save(Menu entity) {
        MenuEntity jpaEntity = mapper.toEntity(entity);
        MenuEntity saved = jpaRepository.save(jpaEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public Menu update(Menu entity) {
        return save(entity);
    }

    @Override
    public List<Menu> findAll() {
        return jpaRepository.findByVisibleTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> findAllIncludingHidden() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Menu> findById(String id) {
        if (id == null) return Optional.empty();
        return jpaRepository.findById(UUID.fromString(id))
                .filter(MenuEntity::getVisible)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Menu> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .filter(MenuEntity::getVisible)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(String id) {
        if (id != null) {
            jpaRepository.findById(UUID.fromString(id)).ifPresent(entity -> {
                entity.setVisible(false);
                entity.setEnabled(false);
                jpaRepository.save(entity);
            });
        }
    }

    @Override
    public void hardDeleteById(String id) {
        if (id != null) {
            jpaRepository.deleteById(UUID.fromString(id));
        }
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public boolean existsById(String id) {
        if (id == null) return false;
        return jpaRepository.existsById(UUID.fromString(id));
    }

    @Override
    public long count() {
        return jpaRepository.findByVisibleTrue().size();
    }

    @Override
    public List<Menu> findByParentId(String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return findRootMenus();
        }
        return jpaRepository.findByParentIdAndVisibleTrue(UUID.fromString(parentId)).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> findRootMenus() {
        return jpaRepository.findByParentIdIsNullAndVisibleTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}