package com.saas.system.infrastructure.persistence.adapter.business;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.business.ContactType;
import com.saas.system.domain.port.out.business.IContactTypeRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.business.ContactTypeEntity;
import com.saas.system.infrastructure.persistence.mapper.business.ContactTypePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.business.JpaContactTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ContactTypeRepositoryAdapter
        extends BaseJpaRepositoryAdapter<ContactType, ContactTypeEntity, UUID>
        implements IContactTypeRepositoryPort {

    private final JpaContactTypeRepository jpa;

    public ContactTypeRepositoryAdapter(JpaContactTypeRepository jpa,
                                        ContactTypePersistenceMapper mapper) {
        super(jpa, mapper, "Tipo de contacto");
        this.jpa = jpa;
    }

    @Override
    public Optional<ContactType> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
