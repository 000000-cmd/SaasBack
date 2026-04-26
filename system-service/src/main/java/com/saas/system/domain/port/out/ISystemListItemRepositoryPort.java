package com.saas.system.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.SystemListItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ISystemListItemRepositoryPort extends IGenericRepositoryPort<SystemListItem, UUID> {

    List<SystemListItem> findByListId(UUID listId);

    /** Code es unico solo dentro del scope de la lista. */
    Optional<SystemListItem> findByListIdAndCode(UUID listId, String code);

    boolean existsByListIdAndCode(UUID listId, String code);
}
