package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.SystemListItem;
import com.saas.system.domain.port.out.ISystemListItemRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.SystemListItemEntity;
import com.saas.system.infrastructure.persistence.mapper.SystemListItemPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaSystemListItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SystemListItemRepositoryAdapter implements ISystemListItemRepositoryPort {

    private final JpaSystemListItemRepository jpa;
    private final SystemListItemPersistenceMapper mapper;

    @Override
    public SystemListItem save(SystemListItem domain) {
        return mapper.toDomain(jpa.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional
    public SystemListItem update(SystemListItem domain) {
        SystemListItemEntity existing = jpa.findById(domain.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SystemListItem", "Id", domain.getId()));
        mapper.updateEntityFromDomain(domain, existing);
        return mapper.toDomain(jpa.save(existing));
    }

    @Override public Optional<SystemListItem> findById(UUID id)         { return jpa.findById(id).map(mapper::toDomain); }
    @Override public boolean                  existsById(UUID id)       { return jpa.existsById(id); }
    @Override public List<SystemListItem>     findAll()                  { return mapper.toDomainList(jpa.findAll()); }
    @Override public List<SystemListItem>     findByListId(UUID listId)  { return mapper.toDomainList(jpa.findByListId(listId)); }
    @Override public Optional<SystemListItem> findByListIdAndCode(UUID listId, String code) {
        return jpa.findByListIdAndCode(listId, code).map(mapper::toDomain);
    }
    @Override public boolean existsByListIdAndCode(UUID listId, String code) {
        return jpa.existsByListIdAndCode(listId, code);
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
