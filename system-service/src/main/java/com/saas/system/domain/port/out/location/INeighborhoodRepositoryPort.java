package com.saas.system.domain.port.out.location;

import com.saas.common.port.out.ICodeRepositoryPort;
import com.saas.system.domain.model.location.Neighborhood;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface INeighborhoodRepositoryPort extends ICodeRepositoryPort<Neighborhood, UUID> {

    List<Neighborhood> findByMunicipalityId(UUID municipalityId);

    List<Neighborhood> findByMunicipalityIdAndType(UUID municipalityId, String type);

    Optional<Neighborhood> findByMunicipalityIdAndCode(UUID municipalityId, String code);

    boolean existsByMunicipalityIdAndCode(UUID municipalityId, String code);
}
