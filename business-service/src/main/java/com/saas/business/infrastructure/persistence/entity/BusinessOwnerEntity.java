package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "business_owner")
@SQLRestriction("Visible = 1")
public class BusinessOwnerEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "ThirdPartyId", length = 36, nullable = false)
    private UUID thirdPartyId;
    @Column(name = "OwnershipPercentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal ownershipPercentage;
    @Column(name = "StartDate", nullable = false) private LocalDate startDate;
    @Column(name = "EndDate") private LocalDate endDate;
}
