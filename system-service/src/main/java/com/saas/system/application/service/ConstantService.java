package com.saas.system.application.service;

import com.saas.common.service.GenericCrudService;
import com.saas.system.domain.model.Constant;
import com.saas.system.domain.port.in.IConstantUseCase;
import com.saas.system.domain.port.out.IConstantRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación para gestión de Constantes.
 */
@Service
public class ConstantService extends GenericCrudService<Constant, String> implements IConstantUseCase {

    private final IConstantRepositoryPort constantRepository;

    public ConstantService(IConstantRepositoryPort repository) {
        super(repository);
        this.constantRepository = repository;
    }

    @Override
    protected String getResourceName() {
        return "Constante";
    }

    @Override
    @Transactional(readOnly = true)
    public List<Constant> getByCategory(String category) {
        return constantRepository.findByCategory(category);
    }
}