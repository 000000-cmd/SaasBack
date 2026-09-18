package com.saas.auth.infrastructure.persistence.repository;

import com.saas.auth.infrastructure.persistence.entity.UserDeviceLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserDeviceLinkRepository extends JpaRepository<UserDeviceLinkEntity, UUID> {

    @Query("SELECT l FROM UserDeviceLinkEntity l WHERE l.user.id = :userId AND l.deviceId = :deviceId")
    Optional<UserDeviceLinkEntity> findLink(@Param("userId") UUID userId,
                                            @Param("deviceId") String deviceId);

    @Query("SELECT l FROM UserDeviceLinkEntity l "
         + "WHERE l.user.id = :userId AND l.revokedAt IS NULL AND l.enabled = true "
         + "ORDER BY l.lastSeenAt DESC")
    List<UserDeviceLinkEntity> activeOfUser(@Param("userId") UUID userId);

    @Query("SELECT l FROM UserDeviceLinkEntity l "
         + "WHERE l.deviceId = :deviceId AND l.revokedAt IS NULL AND l.enabled = true "
         + "ORDER BY l.lastSeenAt DESC")
    List<UserDeviceLinkEntity> activeOfDevice(@Param("deviceId") String deviceId);

    @Modifying
    @Query("UPDATE UserDeviceLinkEntity l SET l.revokedAt = :now, l.revokedBy = :by "
         + "WHERE l.id = :id AND l.revokedAt IS NULL")
    int revoke(@Param("id") UUID id, @Param("by") UUID by, @Param("now") LocalDateTime now);
}
