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
@Entity @Table(name = "whatsapp_session")
@SQLRestriction("Visible = 1")
public class WhatsappSessionEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @Column(name = "PhoneE164", length = 24, nullable = false) private String phoneE164;
    @Column(name = "FlowCode", length = 40, nullable = false) private String flowCode = "AGEND";
    @Column(name = "StepCode", length = 40) private String stepCode;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "DataJson") private String dataJson;
    @Column(name = "ExpiresAt", nullable = false) private LocalDateTime expiresAt;
}
