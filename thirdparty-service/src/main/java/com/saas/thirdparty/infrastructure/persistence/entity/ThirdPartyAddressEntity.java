package com.saas.thirdparty.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "third_party_address")
@SQLRestriction("Visible = 1")
public class ThirdPartyAddressEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ThirdPartyId", length = 36, nullable = false)
    private UUID thirdPartyId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "AddressTypeId", length = 36)
    private UUID addressTypeId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "MunicipalityId", length = 36, nullable = false)
    private UUID municipalityId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "NeighborhoodId", length = 36)
    private UUID neighborhoodId;

    @Column(name = "Line", length = 255)
    private String line;

    @Column(name = "Reference", length = 255)
    private String reference;

    @Column(name = "IsPrimary", nullable = false)
    private Boolean isPrimary = Boolean.FALSE;
}
