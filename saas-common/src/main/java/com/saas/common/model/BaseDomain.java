package com.saas.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de dominio base. Espejo de {@link com.saas.common.persistence.BaseEntity}
 * pero libre de anotaciones JPA (es la representacion de negocio, agnostica de
 * persistencia).
 *
 * Los campos de auditoria son SOLO LECTURA desde la perspectiva del dominio: se
 * llenan automaticamente al persistir la entidad y se leen al mapear de vuelta.
 * Ningun caso de uso debe escribir directamente {@code auditUser} / {@code auditDate}
 * / {@code createdDate}.
 */
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString(of = "id")
public abstract class BaseDomain implements IIdentifiable<UUID> {

    private UUID id;
    private Boolean enabled = Boolean.TRUE;
    private Boolean visible = Boolean.TRUE;
    private UUID auditUser;
    private LocalDateTime auditDate;
    private LocalDateTime createdDate;

    /**
     * Marca la entidad como soft-deleted. Util en operaciones de eliminacion
     * que no eliminan fisicamente. El AuditUser/AuditDate los rellena el
     * listener JPA al persistir.
     */
    public void softDelete() {
        this.enabled = Boolean.FALSE;
        this.visible = Boolean.FALSE;
    }
}
