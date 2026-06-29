package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "business_schedule")
@SQLRestriction("Visible = 1")
public class BusinessScheduleEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "ScheduleTypeId", length = 36, nullable = false)
    private UUID scheduleTypeId;
    @Column(name = "Name", length = 120, nullable = false) private String name;
    @Column(name = "ValidFrom", nullable = false) private LocalDateTime validFrom;
    @Column(name = "ValidTo") private LocalDateTime validTo;
}
