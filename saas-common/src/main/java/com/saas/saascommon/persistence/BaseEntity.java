package com.saas.saascommon.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "Enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "AuditUser", nullable = false)
    private String auditUser;

    @Column(name = "AuditDate", nullable = false)
    private LocalDateTime auditDate;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        this.auditDate = LocalDateTime.now();

        // TODO: Cuando integres Spring Security, aquí obtendrás el usuario real
        if (this.auditUser == null) {
            this.auditUser = "SYSTEM_USER";
        }
    }
}
