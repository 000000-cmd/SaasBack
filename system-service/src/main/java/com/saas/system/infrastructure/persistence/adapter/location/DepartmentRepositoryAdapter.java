package com.saas.system.infrastructure.persistence.adapter.location;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.location.Department;
import com.saas.system.domain.port.out.location.IDepartmentRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.location.DepartmentEntity;
import com.saas.system.infrastructure.persistence.mapper.location.DepartmentPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.location.JpaDepartmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DepartmentRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Department, DepartmentEntity, UUID>
        implements IDepartmentRepositoryPort {

    private final JpaDepartmentRepository jpa;

    public DepartmentRepositoryAdapter(JpaDepartmentRepository jpa, DepartmentPersistenceMapper mapper) {
        super(jpa, mapper, "Departamento");
        this.jpa = jpa;
    }

    @Override
    public Optional<Department> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    @Override
    public List<Department> findByCountryId(UUID countryId) {
        return getMapper().toDomainList(jpa.findByCountryId(countryId));
    }

    @Override
    public Optional<Department> findByCountryIdAndCode(UUID countryId, String code) {
        return jpa.findByCountryIdAndCode(countryId, code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCountryIdAndCode(UUID countryId, String code) {
        return jpa.existsByCountryIdAndCode(countryId, code);
    }
}
