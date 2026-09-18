package com.saas.business.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

/**
 * El cerrojo de la agenda: una fila por empleado y dia local.
 *
 * <p>Sin filtro de visibilidad, a diferencia del resto de entidades. Esta fila
 * no es un dato de negocio: es un cerrojo, y tiene que encontrarse SIEMPRE. Uno
 * que a veces no aparece porque alguien lo marco invisible deja de ser un
 * cerrojo y se convierte en dos citas a la misma hora.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "agenda_lock")
public class AgendaLockEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "EmployeeId", length = 36, nullable = false)
    private UUID employeeId;

    @Column(name = "LocalDate", nullable = false)
    private LocalDate localDate;
}
