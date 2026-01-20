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
 *
 * NOTA IMPORTANTE sobre UUIDs:
 * Para evitar problemas con scripts SQL, usa @Column con columnDefinition:
 *
 * @Id
 * @UuidGenerator
 * @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
 * private UUID id;
 */
@Data
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "Enabled", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean enabled = true;

    @Column(name = "Visible", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean visible = true;

    @Column(name = "AuditUser", nullable = false, length = 100)
    private String auditUser;

    @Column(name = "AuditDate", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
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