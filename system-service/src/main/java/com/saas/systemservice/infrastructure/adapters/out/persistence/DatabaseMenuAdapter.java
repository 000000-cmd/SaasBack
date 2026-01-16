package com.saas.systemservice.infrastructure.adapters.out.persistence;

import com.saas.systemservice.domain.model.menu.Menu;
import com.saas.systemservice.domain.ports.out.menu.IMenuRepositoryPort;
import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.menu.MenuEntity;
import com.saas.systemservice.infrastructure.adapters.out.persistence.mapper.menu.MenuPersistenceMapper;
import com.saas.systemservice.infrastructure.adapters.out.persistence.repository.menu.JpaMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DatabaseMenuAdapter implements IMenuRepositoryPort {

    private final JpaMenuRepository jpaRepository;
    private final MenuPersistenceMapper mapper;

    @Override
    public Menu save(Menu menu) {
        MenuEntity entity = mapper.toEntity(menu);
        MenuEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Menu update(Menu menu) {
        return save(menu);
    }

    @Override
    public List<Menu> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Menu> findById(String id) {
        if (id == null) return Optional.empty();
        return jpaRepository.findById(UUID.fromString(id))
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Menu> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(String id) {
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
}
