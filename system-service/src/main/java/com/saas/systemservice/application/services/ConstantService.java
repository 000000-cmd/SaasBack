package com.saas.systemservice.application.services;

import com.saas.saascommon.domain.exceptions.BusinessException;
import com.saas.systemservice.domain.model.Constant;
import com.saas.systemservice.domain.ports.in.IConstantUseCase;
import com.saas.systemservice.domain.ports.out.IConstantRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ConstantService extends GenericCrudService<Constant, String> implements IConstantUseCase {

    public ConstantService(IConstantRepositoryPort repository) {
        super(repository);
    }
}
