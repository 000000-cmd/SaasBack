package com.saas.common.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Clase base para todos los modelos de dominio.
 * Contiene campos de auditoría y estado comunes.
 */
@Data
public abstract class BaseDomain {

    private Boolean enabled = true;
    private Boolean visible = true;
    private String auditUser;
    private LocalDateTime auditDate;

    /**
     * Marca la entidad como creada por un usuario específico
     */
    public void markAsCreated(String username) {
        this.auditUser = username;
        this.auditDate = LocalDateTime.now();
        this.enabled = true;
        this.visible = true;
    }

    /**
     * Marca la entidad como actualizada por un usuario específico
     */
    public void markAsUpdated(String username) {
        this.auditUser = username;
        this.auditDate = LocalDateTime.now();
    }

    /**
     * Soft delete - marca como no visible
     */
    public void markAsDeleted(String username) {
        this.visible = false;
        this.enabled = false;
        markAsUpdated(username);
    }
}