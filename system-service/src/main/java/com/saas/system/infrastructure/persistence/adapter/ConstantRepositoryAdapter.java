package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.Constant;
import com.saas.system.domain.port.out.IConstantRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.ConstantEntity;
import com.saas.system.infrastructure.persistence.mapper.ConstantPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaConstantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ConstantRepositoryAdapter implements IConstantRepositoryPort {

    private final JpaConstantRepository jpa;
    private final ConstantPersistenceMapper mapper;

    @Override
    public Constant save(Constant domain) {
        return mapper.toDomain(jpa.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional
    public Constant update(Constant domain) {
        ConstantEntity existing = jpa.findById(domain.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Constant", "Id", domain.getId()));
        mapper.updateEntityFromDomain(domain, existing);
        return mapper.toDomain(jpa.save(existing));
    }

    @Override public Optional<Constant> findById(UUID id)        { return jpa.findById(id).map(mapper::toDomain); }
    @Override public boolean            existsById(UUID id)      { return jpa.existsById(id); }
    @Override public List<Constant>     findAll()                 { return mapper.toDomainList(jpa.findAll()); }
    @Override public Optional<Constant> findByCode(String code)  { return jpa.findByCode(code).map(mapper::toDomain); }
    @Override public boolean            existsByCode(String code){ return jpa.existsByCode(code); }

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
