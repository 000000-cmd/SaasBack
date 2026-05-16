package com.saas.system.infrastructure.persistence.repository.location;

import com.saas.system.infrastructure.persistence.entity.location.MunicipalityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaMunicipalityRepository extends JpaRepository<MunicipalityEntity, UUID> {

    Optional<MunicipalityEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<MunicipalityEntity> findByDepartmentId(UUID departmentId);

    Optional<MunicipalityEntity> findByDepartmentIdAndCode(UUID departmentId, String code);

    boolean existsByDepartmentIdAndCode(UUID departmentId, String code);
}
