package com.saas.business.infrastructure.persistence.entity;

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
@Table(name = "business_domain")
@SQLRestriction("Visible = 1")
public class BusinessDomainEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;

    @Column(name = "Slug", length = 63, nullable = false, unique = true)
    private String slug;

    @Column(name = "CustomDomain", length = 255, unique = true)
    private String customDomain;

    @Column(name = "IsPrimary", nullable = false)
    private Boolean isPrimary;

    @Column(name = "IsVerified", nullable = false)
    private Boolean isVerified;

    @Column(name = "VerifiedDate")
    private LocalDateTime verifiedDate;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "StatusId", length = 36)
    private UUID statusId;
}
