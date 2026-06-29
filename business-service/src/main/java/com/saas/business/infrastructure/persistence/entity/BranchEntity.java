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
@Table(name = "branch")
@SQLRestriction("Visible = 1")
public class BranchEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BranchTypeId", length = 36, nullable = false)
    private UUID branchTypeId;
    @Column(name = "Name", length = 160, nullable = false) private String name;
    @Column(name = "Code", length = 40) private String code;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "MunicipalityId", length = 36, nullable = false)
    private UUID municipalityId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "NeighborhoodId", length = 36)
    private UUID neighborhoodId;
    @Column(name = "AddressLine", length = 255) private String addressLine;
    @Column(name = "Phone", length = 30) private String phone;
    @Column(name = "IsMain", nullable = false) private Boolean isMain = Boolean.FALSE;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "StatusId", length = 36) private UUID statusId;
}
