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
@Table(name = "verification_code")
public class VerificationCodeEntity extends BaseEntity {

    @Column(name = "Target", length = 320, nullable = false)
    private String target;

    @Column(name = "ChannelCode", length = 40, nullable = false)
    private String channelCode;

    @Column(name = "Purpose", length = 40, nullable = false)
    private String purpose;

    @Column(name = "CodeHash", length = 128, nullable = false)
    private String codeHash;

    @Column(name = "ExpiresAt", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "Attempts", nullable = false)
    private Integer attempts;

    @Column(name = "MaxAttempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "ConsumedAt")
    private LocalDateTime consumedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ContactId", length = 36)
    private UUID contactId;
}
