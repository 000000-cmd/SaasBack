package com.saas.system.application.service;

import com.saas.common.events.EventTypes;
import com.saas.common.outbox.OutboxPublisher;
import com.saas.common.service.CodeCrudService;
import com.saas.system.application.dto.event.RoleEventPayload;
import com.saas.system.domain.model.Role;
import com.saas.system.domain.port.in.IRoleUseCase;
import com.saas.system.domain.port.out.IRoleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleService extends CodeCrudService<Role, UUID> implements IRoleUseCase {

    private final IRoleRepositoryPort roleRepo;
    private final OutboxPublisher  outboxPublisher;

    public RoleService(IRoleRepositoryPort repo, OutboxPublisher publisher) {
        super(repo);
        this.roleRepo = repo;
        this.outboxPublisher = publisher;
    }

    @Override protected String getResourceName() { return "Rol"; }

    @Override
    protected void applyChanges(Role existing, Role incoming) {
        if (incoming.getCode() != null)        existing.setCode(incoming.getCode());
        if (incoming.getName() != null)        existing.setName(incoming.getName());
        if (incoming.getDescription() != null) existing.setDescription(incoming.getDescription());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findByIds(Set<UUID> ids) {
        return ids == null || ids.isEmpty() ? List.of() : roleRepo.findAllByIds(ids);
    }

    // ===========================================================
    // Hooks que emiten eventos al outbox (Fase 5)
    // ===========================================================

    @Override
    protected void onAfterCreate(Role saved) {
        outboxPublisher.publish(
                EventTypes.ROLE_CREATED,
                null,
                "role",
                saved.getId(),
                RoleEventPayload.from(saved));
    }

    @Override
    protected void onAfterUpdate(Role existing, Role updated) {
        outboxPublisher.publish(
                EventTypes.ROLE_UPDATED,
                null,
                "role",
                updated.getId(),
                RoleEventPayload.from(updated));
    }

    @Override
    protected void onAfterDelete(UUID id, Role deletedSnapshot) {
        outboxPublisher.publish(
                EventTypes.ROLE_DELETED,
                null,
                "role",
                id,
                RoleEventPayload.from(deletedSnapshot));
    }
}
