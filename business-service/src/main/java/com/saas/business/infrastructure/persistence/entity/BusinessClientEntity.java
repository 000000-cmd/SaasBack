package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/** Fila de {@code business_client}. El porque de cada columna esta en V1. */
@Getter
@Setter
@Entity
@Table(name = "business_client",
       uniqueConstraints = @UniqueConstraint(name = "uq_bc_business_phone",
                                             columnNames = {"BusinessId", "PhoneE164"}))
@SQLRestriction("Visible = 1")
public class BusinessClientEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;

    /** Referencia al esquema `personas`, sin FK. NULL = cliente de mostrador. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ThirdPartyId", length = 36)
    private UUID thirdPartyId;

    @Column(name = "DisplayName", length = 160, nullable = false)
    private String displayName;

    @Column(name = "PhoneE164", length = 20)
    private String phoneE164;

    @Column(name = "PhoneVerifiedAt")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "WhatsappOptInAt")
    private LocalDateTime whatsappOptInAt;

    @Column(name = "WhatsappOptInText", length = 500)
    private String whatsappOptInText;

    @Column(name = "WhatsappOptOutAt")
    private LocalDateTime whatsappOptOutAt;

    @Column(name = "AcquisitionSource", length = 80)
    private String acquisitionSource;

    @Column(name = "Notes", length = 500)
    private String notes;

    @Column(name = "NoShowCount", nullable = false)
    private Integer noShowCount = 0;

    @Column(name = "VisitCount", nullable = false)
    private Integer visitCount = 0;

    @Column(name = "LastVisitAt")
    private LocalDateTime lastVisitAt;
}
