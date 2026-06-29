package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.Business;
import com.saas.business.domain.port.out.IBusinessRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BusinessEntity;
import com.saas.business.infrastructure.persistence.mapper.BusinessPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBusinessRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class BusinessRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Business, BusinessEntity, UUID>
        implements IBusinessRepositoryPort {

    public BusinessRepositoryAdapter(JpaBusinessRepository jpa, BusinessPersistenceMapper mapper) {
        super(jpa, mapper, "Empresa");
    }
}
