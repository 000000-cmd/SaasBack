package com.saas.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Clase base para todas las entidades JPA.
 * Maneja automáticamente los campos de auditoría.
 */
@Data
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "Enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "Visible", nullable = false)
    private Boolean visible = true;

    @Column(name = "AuditUser", nullable = false, length = 100)
    private String auditUser;

    @Column(name = "AuditDate", nullable = false)
    private LocalDateTime auditDate;

    @PrePersist
    protected void onCreate() {
        this.auditDate = LocalDateTime.now();
        if (this.enabled == null) this.enabled = true;
        if (this.visible == null) this.visible = true;
        if (this.auditUser == null) this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.auditDate = LocalDateTime.now();
    }
}