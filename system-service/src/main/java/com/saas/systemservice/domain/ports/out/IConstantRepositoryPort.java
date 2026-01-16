package com.saas.systemservice.domain.ports.out;

import com.saas.systemservice.domain.model.Constant;

public interface IConstantRepositoryPort extends IGenericRepositoryPort<Constant, String> {

    void updateEnabled(String id, boolean enabled);
}
