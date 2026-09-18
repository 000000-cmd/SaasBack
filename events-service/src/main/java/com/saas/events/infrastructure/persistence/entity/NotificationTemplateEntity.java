package com.saas.events.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "notification_template")
@SQLRestriction("Visible = 1")
public class NotificationTemplateEntity extends BaseEntity {

    @Column(name = "Code", nullable = false, length = 80)
    private String code;

    @Column(name = "Name", nullable = false, length = 120)
    private String name;

    @Column(name = "TypeCode", nullable = false, length = 40)
    private String typeCode;

    @Column(name = "Subject", length = 300)
    private String subject;

    @Lob
    @Column(name = "Body", nullable = false, columnDefinition = "LONGTEXT")
    private String body;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "NotificationId", length = 36)
    private UUID notificationId;
}
