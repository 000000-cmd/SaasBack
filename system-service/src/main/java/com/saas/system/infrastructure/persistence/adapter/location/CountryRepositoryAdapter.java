package com.saas.system.infrastructure.persistence.adapter.location;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.location.Country;
import com.saas.system.domain.port.out.location.ICountryRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.location.CountryEntity;
import com.saas.system.infrastructure.persistence.mapper.location.CountryPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.location.JpaCountryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class CountryRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Country, CountryEntity, UUID>
        implements ICountryRepositoryPort {

    private final JpaCountryRepository jpa;

    public CountryRepositoryAdapter(JpaCountryRepository jpa, CountryPersistenceMapper mapper) {
        super(jpa, mapper, "Pais");
        this.jpa = jpa;
    }

    @Override
    public Optional<Country> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
