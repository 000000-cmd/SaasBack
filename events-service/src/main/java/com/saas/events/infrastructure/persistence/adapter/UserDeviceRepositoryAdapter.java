package com.saas.events.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.events.domain.model.UserDevice;
import com.saas.events.domain.port.out.IUserDeviceRepositoryPort;
import com.saas.events.infrastructure.persistence.entity.UserDeviceEntity;
import com.saas.events.infrastructure.persistence.mapper.UserDevicePersistenceMapper;
import com.saas.events.infrastructure.persistence.repository.JpaUserDeviceRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserDeviceRepositoryAdapter
        extends BaseJpaRepositoryAdapter<UserDevice, UserDeviceEntity, UUID>
        implements IUserDeviceRepositoryPort {

    private final JpaUserDeviceRepository jpa;

    public UserDeviceRepositoryAdapter(JpaUserDeviceRepository jpa, UserDevicePersistenceMapper mapper) {
        super(jpa, mapper, "UserDevice");
        this.jpa = jpa;
    }

    @Override
    public Optional<UserDevice> findByToken(String fcmToken) {
        return jpa.findByFcmToken(fcmToken).map(getMapper()::toDomain);
    }

    @Override
    public List<UserDevice> findByOwner(UUID thirdPartyId) {
        return jpa.findByThirdPartyId(thirdPartyId).stream().map(getMapper()::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByToken(String fcmToken) {
        jpa.deleteByFcmToken(fcmToken);
    }
}
