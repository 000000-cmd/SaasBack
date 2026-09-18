package com.saas.auth.infrastructure.persistence.adapter;

import com.saas.auth.domain.model.UserDeviceLink;
import com.saas.auth.domain.port.out.IUserDeviceLinkRepositoryPort;
import com.saas.auth.infrastructure.persistence.entity.UserDeviceLinkEntity;
import com.saas.auth.infrastructure.persistence.mapper.UserDeviceLinkPersistenceMapper;
import com.saas.auth.infrastructure.persistence.repository.JpaUserDeviceLinkRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserDeviceLinkRepositoryAdapter
        extends BaseJpaRepositoryAdapter<UserDeviceLink, UserDeviceLinkEntity, UUID>
        implements IUserDeviceLinkRepositoryPort {

    private final JpaUserDeviceLinkRepository jpa;

    public UserDeviceLinkRepositoryAdapter(JpaUserDeviceLinkRepository jpa,
                                           UserDeviceLinkPersistenceMapper mapper) {
        super(jpa, mapper, "Vinculacion de dispositivo");
        this.jpa = jpa;
    }

    @Override
    public Optional<UserDeviceLink> find(UUID userId, String deviceId) {
        return jpa.findLink(userId, deviceId).map(getMapper()::toDomain);
    }

    @Override
    public List<UserDeviceLink> activeOfUser(UUID userId) {
        return getMapper().toDomainList(jpa.activeOfUser(userId));
    }

    @Override
    public List<UserDeviceLink> activeOfDevice(String deviceId) {
        return getMapper().toDomainList(jpa.activeOfDevice(deviceId));
    }

    @Override
    @Transactional
    public int revoke(UUID id, UUID revokedBy) {
        return jpa.revoke(id, revokedBy, LocalDateTime.now());
    }
}
