package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.SystemList;
import com.saas.system.domain.port.out.ISystemListRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.SystemListEntity;
import com.saas.system.infrastructure.persistence.mapper.SystemListPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaSystemListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SystemListRepositoryAdapter implements ISystemListRepositoryPort {

    private final JpaSystemListRepository jpa;
    private final SystemListPersistenceMapper mapper;

    @Override
    public SystemList save(SystemList domain) {
        return mapper.toDomain(jpa.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional
    public SystemList update(SystemList domain) {
        SystemListEntity existing = jpa.findById(domain.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SystemList", "Id", domain.getId()));
        mapper.updateEntityFromDomain(domain, existing);
        return mapper.toDomain(jpa.save(existing));
    }

    @Override public Optional<SystemList> findById(UUID id)        { return jpa.findById(id).map(mapper::toDomain); }
    @Override public boolean              existsById(UUID id)      { return jpa.existsById(id); }
    @Override public List<SystemList>     findAll()                 { return mapper.toDomainList(jpa.findAll()); }
    @Override public Optional<SystemList> findByCode(String code)  { return jpa.findByCode(code).map(mapper::toDomain); }
    @Override public boolean              existsByCode(String code){ return jpa.existsByCode(code); }

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
