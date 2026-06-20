package com.saas.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.saas.common.audit.AuditAction;
import com.saas.common.audit.AuditEmitter;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import com.saas.common.port.in.IGenericUseCase;
import com.saas.common.port.out.IGenericRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementacion base para servicios CRUD basados en Id.
 *
 * Patron de uso:
 *   1. El servicio concreto extiende esta clase y provee {@link #getResourceName()}.
 *   2. Para validaciones extra al crear o actualizar, sobrescribe los hooks
 *      {@link #onBeforeCreate(BaseDomain)} y {@link #onBeforeUpdate(BaseDomain, BaseDomain)}.
 *   3. La auditoria (AuditUser, AuditDate, CreatedDate) es transparente: la
 *      maneja {@code AuditingEntityListener} via {@code AuditorAwareImpl}, NO
 *      este servicio.
 *
 * @param <T>  tipo de dominio
 * @param <ID> tipo del identificador
 */
@Slf4j
@RequiredArgsConstructor
public abstract class GenericCrudService<T extends BaseDomain, ID>
        implements IGenericUseCase<T, ID> {

    protected final IGenericRepositoryPort<T, ID> repository;

    /**
     * Auditoria central: inyectada por campo (no por constructor) para no
     * obligar a las subclases a cambiar su constructor. Opcional: si el
     * servicio no tiene outbox, queda null y no se audita.
     */
    @Autowired(required = false)
    protected AuditEmitter auditEmitter;

    /** Nombre legible del recurso para mensajes de error (ej. "Rol", "Menu"). */
    protected abstract String getResourceName();

    /**
     * Aplica los cambios del payload entrante sobre la entidad existente.
     * Cada servicio concreto decide que campos son mutables. Esto preserva
     * automaticamente {@code Id}, {@code CreatedDate} y campos no listados.
     *
     * Tipico: usar un mapper MapStruct con @MappingTarget.
     */
    protected abstract void applyChanges(T existing, T incoming);

    /** Hook opcional para validacion previa a crear (ej. unicidad por codigo). */
    protected void onBeforeCreate(T entity) { }

    /** Hook opcional para validacion previa a actualizar. */
    protected void onBeforeUpdate(T existing, T incoming) { }

    /** Hook opcional post-create (despues del save). Tipico: emitir evento al outbox. */
    protected void onAfterCreate(T saved) { }

    /** Hook opcional post-update. Tipico: emitir evento al outbox. */
    protected void onAfterUpdate(T existing, T updated) { }

    /** Hook opcional post-delete (soft). Tipico: emitir evento al outbox. */
    protected void onAfterDelete(ID id, T deletedSnapshot) { }

    @Override
    @Transactional
    public T create(T entity) {
        log.debug("Creando {}", getResourceName());
        onBeforeCreate(entity);
        T saved = repository.save(entity);
        log.info("{} creado: id={}", getResourceName(), saved.getId());
        onAfterCreate(saved);
        audit(AuditAction.CREATE, null, saved);
        return saved;
    }

    @Override
    @Transactional
    public T update(ID id, T incoming) {
        log.debug("Actualizando {} id={}", getResourceName(), id);
        T existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(getResourceName(), "Id", id));
        // Congelar el "before" ANTES de mutar existing en applyChanges.
        JsonNode before = auditEmitter != null ? auditEmitter.snapshot(existing) : null;
        onBeforeUpdate(existing, incoming);
        applyChanges(existing, incoming);
        T updated = repository.update(existing);
        log.info("{} actualizado: id={}", getResourceName(), id);
        onAfterUpdate(existing, updated);
        audit(AuditAction.UPDATE, before, updated);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public T getById(ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(getResourceName(), "Id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAll() {
        return repository.findAll();
    }

    @Transactional
    public void delete(ID id) {
        T snapshot = getById(id);
        repository.softDeleteById(id);
        log.info("{} eliminado (soft) id={}", getResourceName(), id);
        onAfterDelete(id, snapshot);
        // Pasamos el dominio (no un JsonNode) para poder derivar aggregateType/id;
        // softDeleteById opera por id en BD y no muta el snapshot en memoria.
        audit(AuditAction.DELETE, snapshot, null);
    }

    @Override
    @Transactional
    public void toggleEnabled(ID id, boolean enabled) {
        T entity = getById(id);
        JsonNode before = auditEmitter != null ? auditEmitter.snapshot(entity) : null;
        entity.setEnabled(enabled);
        T updated = repository.update(entity);
        log.info("{} id={} -> Enabled={}", getResourceName(), id, enabled);
        onAfterUpdate(entity, updated);
        audit(AuditAction.TOGGLE, before, updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAllPaged(int page, int size) {
        return repository.findAllPaged(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    /**
     * Emite el evento de auditoria. {@code before} puede ser un BaseDomain o un
     * snapshot {@link JsonNode} ya congelado; {@code after} el estado posterior.
     * El aggregateType se deriva del nombre simple del dominio (Role -> "role").
     */
    private void audit(AuditAction action, Object before, T after) {
        if (auditEmitter == null) return;
        T reference = after != null ? after : asDomain(before);
        if (reference == null) return;
        String aggregateType = reference.getClass().getSimpleName().toLowerCase();
        UUID businessId = (reference instanceof ITenantOwned t) ? t.getBusinessId() : null;
        auditEmitter.emit(action, aggregateType, reference.getId(), businessId, before, after);
    }

    @SuppressWarnings("unchecked")
    private T asDomain(Object o) {
        return (o instanceof BaseDomain) ? (T) o : null;
    }

}
