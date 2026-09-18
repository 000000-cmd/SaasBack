package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "business_public_profile")
@SQLRestriction("Visible = 1")
public class BusinessPublicProfileEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @Column(name = "IsListed", nullable = false) private Boolean isListed = Boolean.FALSE;
    @Column(name = "IsVerified", nullable = false) private Boolean isVerified = Boolean.FALSE;
    @Column(name = "ReviewCount", nullable = false) private Integer reviewCount = 0;
    @Column(name = "StarSum", nullable = false) private Integer starSum = 0;
}
