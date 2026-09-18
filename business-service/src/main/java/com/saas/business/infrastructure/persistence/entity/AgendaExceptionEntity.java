package com.saas.business.infrastructure.persistence.entity;

import com.saas.business.domain.model.AgendaExceptionKind;
import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "agenda_exception")
@SQLRestriction("Visible = 1")
public class AgendaExceptionEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BranchId", length = 36)
    private UUID branchId;

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "EmployeeId", length = 36)
    private UUID employeeId;

    @Column(name = "StartUtc", nullable = false) private Instant startUtc;
    @Column(name = "EndUtc", nullable = false) private Instant endUtc;

    @Enumerated(EnumType.STRING) @Column(name = "Kind", length = 16, nullable = false)
    private AgendaExceptionKind kind = AgendaExceptionKind.BLOCK;

    @Column(name = "Reason", length = 200) private String reason;
}
