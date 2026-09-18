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
@Entity @Table(name = "appointment_review")
@SQLRestriction("Visible = 1")
public class AppointmentReviewEntity extends BaseEntity {
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "AppointmentId", length = 36, nullable = false)
    private UUID appointmentId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "BusinessId", length = 36, nullable = false)
    private UUID businessId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "EmployeeId", length = 36, nullable = false)
    private UUID employeeId;
    // La columna es TINYINT (una nota va de 1 a 5; un INT serian tres bytes de
    // mas por fila). El tipo Java sigue siendo Integer para que quien llame no
    // tenga que pensar en bytes, y @JdbcTypeCode es lo que evita que la
    // validacion de esquema al arrancar cante la diferencia.
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "BusinessStars", nullable = false) private Integer businessStars;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "EmployeeStars") private Integer employeeStars;
    @Column(name = "Comment", length = 1000) private String comment;
    @Column(name = "Status", length = 16, nullable = false) private String status = "PUBLISHED";
    @Column(name = "BusinessReply", length = 1000) private String businessReply;
    @Column(name = "BusinessRepliedAt") private LocalDateTime businessRepliedAt;
}
