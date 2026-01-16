package com.saas.systemservice.application.services;

import com.saas.saascommon.domain.exceptions.BusinessException;
import com.saas.saascommon.domain.exceptions.ResourceNotFoundException;
import com.saas.saascommon.model.BaseDomain;
import com.saas.saascommon.model.IBusinessEntity;
import com.saas.systemservice.domain.ports.in.IGenericUseCase;
import com.saas.systemservice.domain.ports.out.IGenericRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
public abstract class GenericCrudService<T extends BaseDomain & IBusinessEntity<ID>, ID>
        implements IGenericUseCase<T, ID> {

    // Inyectamos la interfaz genérica, las clases hijas pasarán la implementación concreta
    protected final IGenericRepositoryPort<T, ID> repository;

    @Override
    @Transactional
    public T create(T entity) {
        if (repository.existsByCode(entity.getCode())) {
            throw new BusinessException("Ya existe un registro con el código: " + entity.getCode());
        }

        // Reglas de negocio globales
        entity.setEnabled(true);
        // Aquí podrías setear visible=true si está en BaseDomain o casteando

        return repository.save(entity);
    }

    @Override
    @Transactional
    public T update(T entity) {
        // Validamos que exista antes de intentar actualizar
        if (entity.getId() == null || !repository.existsById(entity.getId())) {
            // Intentamos buscar por código si el ID es nulo (recuperación)
            T existing = repository.findByCode(entity.getCode())
                    .orElseThrow(() -> new BusinessException("No se encontró el registro para actualizar."));
            entity.setId(existing.getId());
        }
        return repository.update(entity);
    }

    @Override
    public T getByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No encontrado código: " + code));
    }

    @Override
    public T getById(ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No encontrado ID: " + id));
    }

    @Override
    public List<T> getAll() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public void delete(ID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("No se puede eliminar, ID no existe.");
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleEnabled(ID id, boolean enabled) {
        T entity = getById(id);
        entity.setEnabled(enabled);
        repository.update(entity); // Guardamos el cambio de estado
    }
}
