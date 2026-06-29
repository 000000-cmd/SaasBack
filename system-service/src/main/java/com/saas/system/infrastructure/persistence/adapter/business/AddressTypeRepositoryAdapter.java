package com.saas.system.infrastructure.persistence.adapter.business;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.business.AddressType;
import com.saas.system.domain.port.out.business.IAddressTypeRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.business.AddressTypeEntity;
import com.saas.system.infrastructure.persistence.mapper.business.AddressTypePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.business.JpaAddressTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AddressTypeRepositoryAdapter
        extends BaseJpaRepositoryAdapter<AddressType, AddressTypeEntity, UUID>
        implements IAddressTypeRepositoryPort {

    private final JpaAddressTypeRepository jpa;

    public AddressTypeRepositoryAdapter(JpaAddressTypeRepository jpa, AddressTypePersistenceMapper mapper) {
        super(jpa, mapper, "Tipo de direccion");
        this.jpa = jpa;
    }

    @Override
    public Optional<AddressType> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
