package com.saas.system.infrastructure.persistence.adapter.location;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.location.Neighborhood;
import com.saas.system.domain.port.out.location.INeighborhoodRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.location.NeighborhoodEntity;
import com.saas.system.infrastructure.persistence.mapper.location.NeighborhoodPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.location.JpaNeighborhoodRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NeighborhoodRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Neighborhood, NeighborhoodEntity, UUID>
        implements INeighborhoodRepositoryPort {

    private final JpaNeighborhoodRepository jpa;

    public NeighborhoodRepositoryAdapter(JpaNeighborhoodRepository jpa, NeighborhoodPersistenceMapper mapper) {
        super(jpa, mapper, "Barrio/Vereda");
        this.jpa = jpa;
    }

    @Override
    public Optional<Neighborhood> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    @Override
    public List<Neighborhood> findByMunicipalityId(UUID municipalityId) {
        return getMapper().toDomainList(jpa.findByMunicipalityId(municipalityId));
    }

    @Override
    public List<Neighborhood> findByMunicipalityIdAndType(UUID municipalityId, String type) {
        return getMapper().toDomainList(jpa.findByMunicipalityIdAndType(municipalityId, type));
    }

    @Override
    public Optional<Neighborhood> findByMunicipalityIdAndCode(UUID municipalityId, String code) {
        return jpa.findByMunicipalityIdAndCode(municipalityId, code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByMunicipalityIdAndCode(UUID municipalityId, String code) {
        return jpa.existsByMunicipalityIdAndCode(municipalityId, code);
    }
}
