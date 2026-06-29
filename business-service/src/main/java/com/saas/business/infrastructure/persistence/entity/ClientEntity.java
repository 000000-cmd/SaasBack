package com.saas.business.infrastructure.persistence.entity;

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
@Table(name = "client", uniqueConstraints = @UniqueConstraint(name = "uq_client_third_party", columnNames = "ThirdPartyId"))
@SQLRestriction("Visible = 1")
public class ClientEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ThirdPartyId", length = 36, nullable = false)
    private UUID thirdPartyId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "RegistrationStatusId", length = 36)
    private UUID registrationStatusId;

    @Column(name = "AcquisitionSource", length = 80)
    private String acquisitionSource;

    @Column(name = "Notes", length = 255)
    private String notes;
}
