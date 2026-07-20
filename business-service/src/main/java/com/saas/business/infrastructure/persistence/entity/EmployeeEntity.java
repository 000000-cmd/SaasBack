package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "employee")
@SQLRestriction("Visible = 1")
public class EmployeeEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "ThirdPartyId", length = 36, nullable = false)
    private UUID thirdPartyId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BranchId", length = 36, nullable = false)
    private UUID branchId;
    // Nullable desde V3: el alta minima crea un "shell" (solo tercero+sede);
    // cargo y fecha llegan despues (APK del empleado o edicion del dueño).
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "PositionId", length = 36)
    private UUID positionId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "SpecialtyId", length = 36)
    private UUID specialtyId;
    @Column(name = "EmployeeCode", length = 40) private String employeeCode;
    @Column(name = "HireDate") private LocalDate hireDate;
    @Column(name = "TerminationDate") private LocalDate terminationDate;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "StatusId", length = 36) private UUID statusId;
}
