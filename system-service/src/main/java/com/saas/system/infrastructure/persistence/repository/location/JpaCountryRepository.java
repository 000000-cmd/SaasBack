package com.saas.system.infrastructure.persistence.repository.location;

import com.saas.system.infrastructure.persistence.entity.location.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCountryRepository extends JpaRepository<CountryEntity, UUID> {

    Optional<CountryEntity> findByCode(String code);

    boolean existsByCode(String code);
}
