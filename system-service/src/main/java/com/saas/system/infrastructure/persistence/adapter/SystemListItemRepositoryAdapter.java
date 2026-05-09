package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.SystemListItem;
import com.saas.system.domain.port.out.ISystemListItemRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.SystemListItemEntity;
import com.saas.system.infrastructure.persistence.mapper.SystemListItemPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaSystemListItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SystemListItemRepositoryAdapter
        extends BaseJpaRepositoryAdapter<SystemListItem, SystemListItemEntity, UUID>
        implements ISystemListItemRepositoryPort {

    private final JpaSystemListItemRepository jpa;

    public SystemListItemRepositoryAdapter(JpaSystemListItemRepository jpa,
                                           SystemListItemPersistenceMapper mapper) {
        super(jpa, mapper, "SystemListItem");
        this.jpa = jpa;
    }

    @Override
    public List<SystemListItem> findByListId(UUID listId) {
        return getMapper().toDomainList(jpa.findByListId(listId));
    }

    @Override
    public Optional<SystemListItem> findByListIdAndCode(UUID listId, String code) {
        return jpa.findByListIdAndCode(listId, code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByListIdAndCode(UUID listId, String code) {
        return jpa.existsByListIdAndCode(listId, code);
    }
}
