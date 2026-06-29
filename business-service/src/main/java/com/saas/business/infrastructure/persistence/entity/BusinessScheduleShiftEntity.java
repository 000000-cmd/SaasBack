package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.time.LocalTime;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "business_schedule_shift")
@SQLRestriction("Visible = 1")
public class BusinessScheduleShiftEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessScheduleId", length = 36, nullable = false)
    private UUID businessScheduleId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "ShiftTypeId", length = 36, nullable = false)
    private UUID shiftTypeId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "DayOfWeekId", length = 36, nullable = false)
    private UUID dayOfWeekId;
    @Column(name = "StartTime", nullable = false) private LocalTime startTime;
    @Column(name = "EndTime", nullable = false) private LocalTime endTime;
    @Column(name = "DisplayOrder", nullable = false) private Integer displayOrder = 0;
}
