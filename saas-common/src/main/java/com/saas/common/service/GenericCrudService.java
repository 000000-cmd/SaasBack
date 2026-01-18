package com.saas.common.service;

import com.saas.common.exception.DuplicateResourceException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.model.BaseDomain;
import com.saas.common.model.IBusinessEntity;
import com.saas.common.port.in.IGenericUseCase;
import com.saas.common.port.out.IGenericRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación base genérica para servicios CRUD.
 * Proporciona operaciones comunes que pueden ser extendidas.
 *
 * @param <T> Tipo del modelo de dominio
 * @param <ID> Tipo del identificador
 */
@Slf4j
@RequiredArgsConstructor
public abstract class GenericCrudService<T extends BaseDomain & IBusinessEntity<ID>, ID>
        implements IGenericUseCase<T, ID> {

    protected final IGenericRepositoryPort<T, ID> repository;

    /**
     * Nombre del recurso para mensajes de error (ej: "Rol", "Menú")
     */
    protected abstract String getResourceName();

    /**
     * Obtiene el usuario actual para auditoría.
     * Puede ser sobrescrito para integrar con Spring Security.
     */
    protected String getCurrentUser() {
        // TODO: Integrar con SecurityContextHolder cuando esté disponible
        return "SYSTEM";
    }

    @Override
    @Transactional
    public T create(T entity) {
        log.debug("Creando {}: {}", getResourceName(), entity.getCode());

        if (repository.existsByCode(entity.getCode())) {
            throw new DuplicateResourceException(getResourceName(), "código", entity.getCode());
        }

        entity.markAsCreated(getCurrentUser());
        T saved = repository.save(entity);

        log.info("{} creado con código: {}", getResourceName(), saved.getCode());
        return saved;
    }

    @Override
    @Transactional
    public T update(ID id, T entity) {
        log.debug("Actualizando {} con ID: {}", getResourceName(), id);

        T existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(getResourceName(), "ID", id));

        // Si cambió el código, verificar que no exista otro con ese código
        if (!existing.getCode().equals(entity.getCode()) && repository.existsByCode(entity.getCode())) {
            throw new DuplicateResourceException(getResourceName(), "código", entity.getCode());
        }

        entity.setId(id);
        entity.markAsUpdated(getCurrentUser());

        // Preservar campos de creación
        entity.setVisible(existing.getVisible());

        T updated = repository.update(entity);
        log.info("{} actualizado con ID: {}", getResourceName(), id);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public T getByCode(String code) {
        log.debug("Buscando {} por código: {}", getResourceName(), code);
        return repository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(getResourceName(), "código", code));
    }

    @Override
    @Transactional(readOnly = true)
    public T getById(ID id) {
        log.debug("Buscando {} por ID: {}", getResourceName(), id);
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(getResourceName(), "ID", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAll() {
        log.debug("Listando todos los {}", getResourceName());
        return repository.findAll();
    }

    @Override
    @Transactional
    public void delete(ID id) {
        log.debug("Eliminando {} con ID: {}", getResourceName(), id);

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(getResourceName(), "ID", id);
        }

        repository.deleteById(id);
        log.info("{} eliminado con ID: {}", getResourceName(), id);
    }

    @Override
    @Transactional
    public void toggleEnabled(ID id, boolean enabled) {
        log.debug("Cambiando estado de {} con ID: {} a enabled={}", getResourceName(), id, enabled);

        T entity = getById(id);
        entity.setEnabled(enabled);
        entity.markAsUpdated(getCurrentUser());
        repository.update(entity);

        log.info("{} con ID: {} ahora está {}", getResourceName(), id, enabled ? "habilitado" : "deshabilitado");
    }
}