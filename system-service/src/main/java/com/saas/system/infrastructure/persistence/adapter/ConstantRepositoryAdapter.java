package com.saas.system.infrastructure.persistence.adapter;

import com.saas.system.domain.model.Constant;
import com.saas.system.domain.port.out.IConstantRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.ConstantEntity;
import com.saas.system.infrastructure.persistence.mapper.ConstantPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaConstantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Constantes.
 */
@Repository
@RequiredArgsConstructor
public class ConstantRepositoryAdapter implements IConstantRepositoryPort {

    private final JpaConstantRepository jpaRepository;
    private final ConstantPersistenceMapper mapper;

    @Override
    public Constant save(Constant entity) {
        ConstantEntity jpaEntity = mapper.toEntity(entity);
        ConstantEntity saved = jpaRepository.save(jpaEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public Constant update(Constant entity) {
        return save(entity);
    }

    @Override
    public List<Constant> findAll() {
        return jpaRepository.findByVisibleTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Constant> findAllIncludingHidden() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Constant> findById(String id) {
        if (id == null) return Optional.empty();
        return jpaRepository.findById(UUID.fromString(id))
                .filter(ConstantEntity::getVisible)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Constant> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .filter(ConstantEntity::getVisible)
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
    public List<Constant> findByCategory(String category) {
        return jpaRepository.findByCategoryAndVisibleTrue(category).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}