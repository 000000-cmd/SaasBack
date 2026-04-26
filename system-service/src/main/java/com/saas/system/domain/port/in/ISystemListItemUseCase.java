package com.saas.system.domain.port.in;

import com.saas.common.port.in.IGenericUseCase;
import com.saas.system.domain.model.SystemListItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ISystemListItemUseCase extends IGenericUseCase<SystemListItem, UUID> {

    List<SystemListItem> getByListId(UUID listId);

    /** Lookup tipico desde frontend: por code de lista y code de item. */
    Optional<SystemListItem> getByListCodeAndItemCode(String listCode, String itemCode);

    SystemListItem createInList(UUID listId, SystemListItem item);
}
