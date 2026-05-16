package com.saas.system.infrastructure.persistence.adapter.location;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.location.Municipality;
import com.saas.system.domain.port.out.location.IMunicipalityRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.location.MunicipalityEntity;
import com.saas.system.infrastructure.persistence.mapper.location.MunicipalityPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.location.JpaMunicipalityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MunicipalityRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Municipality, MunicipalityEntity, UUID>
        implements IMunicipalityRepositoryPort {

    private final JpaMunicipalityRepository jpa;

    public MunicipalityRepositoryAdapter(JpaMunicipalityRepository jpa, MunicipalityPersistenceMapper mapper) {
        super(jpa, mapper, "Municipio");
        this.jpa = jpa;
    }

    @Override
    public Optional<Municipality> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    @Override
    public List<Municipality> findByDepartmentId(UUID departmentId) {
        return getMapper().toDomainList(jpa.findByDepartmentId(departmentId));
    }

    @Override
    public Optional<Municipality> findByDepartmentIdAndCode(UUID departmentId, String code) {
        return jpa.findByDepartmentIdAndCode(departmentId, code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByDepartmentIdAndCode(UUID departmentId, String code) {
        return jpa.existsByDepartmentIdAndCode(departmentId, code);
    }
}
