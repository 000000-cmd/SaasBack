package com.saas.system.infrastructure.persistence.adapter;

import com.saas.system.domain.model.ListDefinition;
import com.saas.system.domain.port.out.IListDefinitionRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.ListDefinitionEntity;
import com.saas.system.infrastructure.persistence.mapper.ListDefinitionPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaListDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para ListDefinition.
 */
@Repository
@RequiredArgsConstructor
public class ListDefinitionRepositoryAdapter implements IListDefinitionRepositoryPort {

    private final JpaListDefinitionRepository jpaRepository;
    private final ListDefinitionPersistenceMapper mapper;

    @Override
    public ListDefinition save(ListDefinition definition) {
        ListDefinitionEntity entity = mapper.toEntity(definition);
        ListDefinitionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public ListDefinition update(ListDefinition definition) {
        return save(definition);
    }

    @Override
    public List<ListDefinition> findAll() {
        return jpaRepository.findByVisibleTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ListDefinition> findAllIncludingHidden() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ListDefinition> findById(String id) {
        if (id == null) return Optional.empty();
        return jpaRepository.findById(UUID.fromString(id))
                .filter(ListDefinitionEntity::getVisible)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ListDefinition> findByPhysicalTableName(String physicalTableName) {
        return jpaRepository.findByPhysicalTableName(physicalTableName)
                .filter(ListDefinitionEntity::getVisible)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByPhysicalTableName(String physicalTableName) {
        return jpaRepository.existsByPhysicalTableName(physicalTableName);
    }

    @Override
    public boolean existsById(String id) {
        if (id == null) return false;
        return jpaRepository.existsById(UUID.fromString(id));
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
}