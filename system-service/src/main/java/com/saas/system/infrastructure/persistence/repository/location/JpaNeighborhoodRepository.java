package com.saas.system.infrastructure.persistence.repository.location;

import com.saas.system.infrastructure.persistence.entity.location.NeighborhoodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaNeighborhoodRepository extends JpaRepository<NeighborhoodEntity, UUID> {

    Optional<NeighborhoodEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<NeighborhoodEntity> findByMunicipalityId(UUID municipalityId);

    List<NeighborhoodEntity> findByMunicipalityIdAndType(UUID municipalityId, String type);

    Optional<NeighborhoodEntity> findByMunicipalityIdAndCode(UUID municipalityId, String code);

    boolean existsByMunicipalityIdAndCode(UUID municipalityId, String code);
}
