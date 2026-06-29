package com.saas.thirdparty.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "third_party_contact")
@SQLRestriction("Visible = 1")
public class ThirdPartyContactEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ThirdPartyId", length = 36, nullable = false)
    private UUID thirdPartyId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ContactTypeId", length = 36, nullable = false)
    private UUID contactTypeId;

    @Column(name = "Value", length = 160, nullable = false)
    private String value;

    @Column(name = "IsPrimary", nullable = false)
    private Boolean isPrimary = Boolean.FALSE;

    @Column(name = "IsVerified", nullable = false)
    private Boolean isVerified = Boolean.FALSE;

    @Column(name = "VerifiedAt")
    private LocalDateTime verifiedAt;

    @Column(name = "Notes", length = 255)
    private String notes;
}
