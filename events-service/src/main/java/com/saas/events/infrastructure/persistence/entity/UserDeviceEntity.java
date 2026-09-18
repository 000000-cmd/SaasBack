package com.saas.events.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "user_device")
public class UserDeviceEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ThirdPartyId", length = 36, nullable = false)
    private UUID thirdPartyId;

    @Column(name = "FcmToken", length = 255, nullable = false)
    private String fcmToken;

    @Column(name = "Platform", length = 16, nullable = false)
    private String platform;

    @Column(name = "AppVersion", length = 32)
    private String appVersion;

    @Column(name = "LastSeenAt")
    private LocalDateTime lastSeenAt;
}
