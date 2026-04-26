package com.saas.common.service;

import com.saas.common.exception.DuplicateResourceException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.model.BaseDomain;
import com.saas.common.model.ICodeable;
import com.saas.common.port.in.ICodeUseCase;
import com.saas.common.port.out.ICodeRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio CRUD para catalogos con {@code Code} unico.
 * Extiende {@link GenericCrudService} aniadiendo:
 *   - Validacion de unicidad por Code en {@code create} y {@code update}.
 *   - Busqueda por Code.
 */
public abstract class CodeCrudService<T extends BaseDomain & ICodeable, ID>
        extends GenericCrudService<T, ID>
        implements ICodeUseCase<T, ID> {

    protected final ICodeRepositoryPort<T, ID> codeRepository;

    protected CodeCrudService(ICodeRepositoryPort<T, ID> repository) {
        super(repository);
        this.codeRepository = repository;
    }

    @Override
    protected void onBeforeCreate(T entity) {
        if (codeRepository.existsByCode(entity.getCode())) {
            throw new DuplicateResourceException(getResourceName(), "Code", entity.getCode());
        }
    }

    @Override
    protected void onBeforeUpdate(T existing, T incoming) {
        if (incoming.getCode() != null
                && !incoming.getCode().equals(existing.getCode())
                && codeRepository.existsByCode(incoming.getCode())) {
            throw new DuplicateResourceException(getResourceName(), "Code", incoming.getCode());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public T getByCode(String code) {
        return codeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(getResourceName(), "Code", code));
    }
}
