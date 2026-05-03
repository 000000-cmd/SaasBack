package com.saas.common.service;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.model.BaseDomain;
import com.saas.common.port.in.IGenericUseCase;
import com.saas.common.port.out.IGenericRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        return saved;
    }

    @Override
    @Transactional
    public T update(ID id, T incoming) {
        log.debug("Actualizando {} id={}", getResourceName(), id);
        T existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(getResourceName(), "Id", id));
        onBeforeUpdate(existing, incoming);
        applyChanges(existing, incoming);
        T updated = repository.update(existing);
        log.info("{} actualizado: id={}", getResourceName(), id);
        onAfterUpdate(existing, updated);
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
    }

    @Override
    @Transactional
    public void toggleEnabled(ID id, boolean enabled) {
        T entity = getById(id);
        entity.setEnabled(enabled);
        T updated = repository.update(entity);
        log.info("{} id={} -> Enabled={}", getResourceName(), id, enabled);
        onAfterUpdate(entity, updated);
    }
}
