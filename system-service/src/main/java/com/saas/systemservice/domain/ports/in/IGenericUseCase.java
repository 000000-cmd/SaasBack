package com.saas.systemservice.domain.ports.in;

import com.saas.saascommon.model.BaseDomain;
import com.saas.saascommon.model.IBusinessEntity;

import java.util.List;

public interface IGenericUseCase<T extends BaseDomain & IBusinessEntity<ID>, ID> {

    T create(T entity);

    T update(T entity);

    T getByCode(String code);

    T getById(ID id);

    List<T> getAll();

    void delete(ID id);

    void toggleEnabled(ID id, boolean enabled);
}