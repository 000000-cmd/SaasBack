package com.saas.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Superclase comun para TODAS las entidades JPA del sistema.
 *
 * Estandar:
 *   - {@code Id}          UUID generado por Hibernate (RFC 4122 v4) y almacenado como CHAR(36).
 *   - {@code Enabled}     Soft-flag de activacion logica.
 *   - {@code Visible}     Soft-flag de visibilidad (soft-delete).
 *   - {@code AuditUser}   UUID del ultimo usuario que modifico el registro (rellenado por
 *                         {@link com.saas.common.audit.AuditorAwareImpl}).
 *   - {@code AuditDate}   Timestamp de ultima modificacion (auto JPA Auditing).
 *   - {@code CreatedDate} Timestamp de creacion, inmutable (auto JPA Auditing).
 *
 * El llenado de los campos de auditoria es transparente: ocurre via
 * {@link AuditingEntityListener} cuando la entidad se persiste o actualiza,
 * por lo que ningun servicio necesita escribir esos campos manualmente.
 */
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString(of = "id")
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "Id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "Enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "Visible", nullable = false)
    private Boolean visible = Boolean.TRUE;

    @LastModifiedBy
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "AuditUser", length = 36)
    private UUID auditUser;

    @LastModifiedDate
    @Column(name = "AuditDate", nullable = false)
    private LocalDateTime auditDate;

    @CreatedDate
    @Column(name = "CreatedDate", nullable = false, updatable = false)
    private LocalDateTime createdDate;
}
