package com.saas.system.application.service;

import com.saas.common.exception.DuplicateResourceException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.service.GenericCrudService;
import com.saas.system.domain.model.SystemListItem;
import com.saas.system.domain.port.in.ISystemListItemUseCase;
import com.saas.system.domain.port.out.ISystemListItemRepositoryPort;
import com.saas.system.domain.port.out.ISystemListRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SystemListItemService extends GenericCrudService<SystemListItem, UUID> implements ISystemListItemUseCase {

    private final ISystemListItemRepositoryPort itemRepo;
    private final ISystemListRepositoryPort listRepo;

    public SystemListItemService(ISystemListItemRepositoryPort itemRepo,
                                  ISystemListRepositoryPort listRepo) {
        super(itemRepo);
        this.itemRepo = itemRepo;
        this.listRepo = listRepo;
    }

    @Override protected String getResourceName() { return "Item de lista"; }

    @Override
    protected void applyChanges(SystemListItem existing, SystemListItem incoming) {
        if (incoming.getCode() != null)         existing.setCode(incoming.getCode());
        if (incoming.getName() != null)         existing.setName(incoming.getName());
        if (incoming.getValue() != null)        existing.setValue(incoming.getValue());
        if (incoming.getDisplayOrder() != null) existing.setDisplayOrder(incoming.getDisplayOrder());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemListItem> getByListId(UUID listId) {
        if (!listRepo.existsById(listId)) {
            throw new ResourceNotFoundException("Lista del sistema", "Id", listId);
        }
        return itemRepo.findByListId(listId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SystemListItem> getByListCodeAndItemCode(String listCode, String itemCode) {
        return listRepo.findByCode(listCode)
                .flatMap(list -> itemRepo.findByListIdAndCode(list.getId(), itemCode));
    }

    @Override
    @Transactional
    public SystemListItem createInList(UUID listId, SystemListItem item) {
        if (!listRepo.existsById(listId)) {
            throw new ResourceNotFoundException("Lista del sistema", "Id", listId);
        }
        if (itemRepo.existsByListIdAndCode(listId, item.getCode())) {
            throw new DuplicateResourceException("Item de lista", "Code", item.getCode());
        }
        item.setListId(listId);
        return create(item);
    }
}
