package com.saas.systemservice.infrastructure.adapters.out.persistence;

import com.saas.systemservice.domain.model.Constant;
import com.saas.systemservice.domain.ports.out.IConstantRepositoryPort;
import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.ConstantEntity;
import com.saas.systemservice.infrastructure.adapters.out.persistence.mapper.ConstantPersistenceMapper;
import com.saas.systemservice.infrastructure.adapters.out.persistence.repository.JpaConstantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DatabaseConstantAdapter implements IConstantRepositoryPort {

    private final JpaConstantRepository jpaRepository;
    private final ConstantPersistenceMapper mapper;

    @Override
    public Constant save(Constant constant) {
        ConstantEntity entity = mapper.toEntity(constant);
        ConstantEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Constant update(Constant constant) {
        return save(constant);
    }

    @Override
    public List<Constant> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Constant> findById(String id) {
        if (id == null) return Optional.empty();
        return jpaRepository.findById(UUID.fromString(id))
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Constant> findByCode(String code) {
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

    @Override
    public void updateEnabled(String id, boolean enabled) {
        if (id != null) {
            jpaRepository.updateEnabledStatus(UUID.fromString(id), enabled);
        }
    }
}