package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.AppVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaAppVersionRepository extends JpaRepository<AppVersionEntity, UUID> {

    Optional<AppVersionEntity> findByIsCurrentTrue();

    Optional<AppVersionEntity> findByVersion(String version);

    boolean existsByVersionCode(Integer versionCode);
}
