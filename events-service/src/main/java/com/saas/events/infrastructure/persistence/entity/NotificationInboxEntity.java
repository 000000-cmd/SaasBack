package com.saas.events.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bandeja del destinatario. Sin {@code @SQLRestriction}: la bandeja se consulta
 * entera, incluidas las leidas.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "notification_inbox")
public class NotificationInboxEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ThirdPartyId", length = 36, nullable = false)
    private UUID thirdPartyId;

    @Column(name = "NotificationCode", length = 80)
    private String notificationCode;

    @Column(name = "TemplateCode", length = 80)
    private String templateCode;

    @Column(name = "TypeCode", length = 40)
    private String typeCode;

    @Column(name = "Title", length = 300)
    private String title;

    @Lob
    @Column(name = "Body", nullable = false, columnDefinition = "LONGTEXT")
    private String body;

    @Column(name = "ReadAt")
    private LocalDateTime readAt;
}
