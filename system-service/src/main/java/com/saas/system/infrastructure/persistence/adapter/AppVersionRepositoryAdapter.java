package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.AppVersion;
import com.saas.system.domain.port.out.IAppVersionRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.AppVersionEntity;
import com.saas.system.infrastructure.persistence.mapper.AppVersionPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaAppVersionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AppVersionRepositoryAdapter
        extends BaseJpaRepositoryAdapter<AppVersion, AppVersionEntity, UUID>
        implements IAppVersionRepositoryPort {

    private final JpaAppVersionRepository jpa;

    public AppVersionRepositoryAdapter(JpaAppVersionRepository jpa, AppVersionPersistenceMapper mapper) {
        super(jpa, mapper, "Version del APK");
        this.jpa = jpa;
    }

    @Override
    public Optional<AppVersion> findCurrent() {
        return jpa.findByIsCurrentTrue().map(getMapper()::toDomain);
    }

    @Override
    public Optional<AppVersion> findByVersion(String version) {
        return jpa.findByVersion(version).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByVersionCode(Integer versionCode) {
        return jpa.existsByVersionCode(versionCode);
    }

    @Override
    public void hardDeleteById(UUID id) {
        jpa.deleteById(id);
    }
}
