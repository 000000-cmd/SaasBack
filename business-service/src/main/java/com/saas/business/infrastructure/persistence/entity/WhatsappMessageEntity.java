package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "whatsapp_message")
@SQLRestriction("Visible = 1")
public class WhatsappMessageEntity extends BaseEntity {
    @Column(name = "WaMessageId", length = 120, nullable = false) private String waMessageId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36)
    private UUID businessId;
    @Column(name = "FromPhone", length = 24, nullable = false) private String fromPhone;
    @Column(name = "ToPhone", length = 24) private String toPhone;
    @Column(name = "Body", columnDefinition = "text") private String body;
    @Column(name = "RawPayload", columnDefinition = "mediumtext", nullable = false)
    private String rawPayload;
    @Column(name = "ReceivedAt", nullable = false) private LocalDateTime receivedAt;
    @Column(name = "ProcessedAt") private LocalDateTime processedAt;
    @Column(name = "Error", length = 500) private String error;
}
