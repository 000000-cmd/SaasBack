package com.saas.system.infrastructure.persistence.repository.location;

import com.saas.system.infrastructure.persistence.entity.location.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaDepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {

    Optional<DepartmentEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<DepartmentEntity> findByCountryId(UUID countryId);

    Optional<DepartmentEntity> findByCountryIdAndCode(UUID countryId, String code);

    boolean existsByCountryIdAndCode(UUID countryId, String code);
}
