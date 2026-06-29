package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "employee_shift_assignment")
@SQLRestriction("Visible = 1")
public class EmployeeShiftAssignmentEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "EmployeeId", length = 36, nullable = false)
    private UUID employeeId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BranchScheduleShiftId", length = 36, nullable = false)
    private UUID branchScheduleShiftId;
    @Column(name = "IsFullShift", nullable = false) private Boolean isFullShift = Boolean.TRUE;
    @Column(name = "CustomStartTime") private LocalTime customStartTime;
    @Column(name = "CustomEndTime") private LocalTime customEndTime;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "StatusId", length = 36) private UUID statusId;
    @Column(name = "ValidFrom", nullable = false) private LocalDateTime validFrom;
    @Column(name = "ValidTo") private LocalDateTime validTo;
}
