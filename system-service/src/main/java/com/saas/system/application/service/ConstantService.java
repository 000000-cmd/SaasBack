package com.saas.system.application.service;

import com.saas.common.service.CodeCrudService;
import com.saas.system.domain.model.Constant;
import com.saas.system.domain.port.in.IConstantUseCase;
import com.saas.system.domain.port.out.IConstantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConstantService extends CodeCrudService<Constant, UUID> implements IConstantUseCase {

    public ConstantService(IConstantRepositoryPort repo) { super(repo); }

    @Override protected String getResourceName() { return "Constante"; }

    @Override
    protected void applyChanges(Constant existing, Constant incoming) {
        if (incoming.getCode() != null)        existing.setCode(incoming.getCode());
        if (incoming.getName() != null)        existing.setName(incoming.getName());
        if (incoming.getValue() != null)       existing.setValue(incoming.getValue());
        if (incoming.getDescription() != null) existing.setDescription(incoming.getDescription());
    }
}
