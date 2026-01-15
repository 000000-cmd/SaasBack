package com.saas.authservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO genérico para las respuestas de las entidades de tipo lista.
 * Define la estructura de datos que se enviará al cliente.
 */
public class ListTypeResponseDTO {
    private UUID id;
    private String code;
    private String name;
    private Integer order;
    private Boolean indicatorEnabled;
    private LocalDateTime auditDate;

    // --- Constructores ---
    public ListTypeResponseDTO() {}

    public ListTypeResponseDTO(UUID id, String code, String name, Integer order, Boolean indicatorEnabled, LocalDateTime auditDate) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.order = order;
        this.indicatorEnabled = indicatorEnabled;
        this.auditDate = auditDate;
    }

    // --- Getters y Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
    public Boolean getIndicatorEnabled() { return indicatorEnabled; }
    public void setIndicatorEnabled(Boolean indicatorEnabled) { this.indicatorEnabled = indicatorEnabled; }
    public LocalDateTime getAuditDate() { return auditDate; }
    public void setAuditDate(LocalDateTime auditDate) { this.auditDate = auditDate; }
}