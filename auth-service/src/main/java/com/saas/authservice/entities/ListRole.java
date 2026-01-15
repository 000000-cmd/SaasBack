package com.saas.authservice.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "listroletypes")
public class ListRole {

    @Id
    @Column(name = "IdList")
    private UUID id;

    @Column(name = "Code", nullable = false, unique = true)
    private String code;

    @Column(name = "Name", nullable = false)
    private String name;

    @Column(name = "`Order`", nullable = false)
    private Integer order;

    @Column(name = "IndicatorEnabled", nullable = false)
    private Boolean indicatorEnabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserAudit")
    @JsonIgnoreProperties({"userAudit"})
    private User userAudit;

    @Column(name = "AuditDate", nullable = false)
    private LocalDateTime auditDate;

    // Getters y Setters
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
    public User getUserAudit() { return userAudit; }
    public void setUserAudit(User userAudit) { this.userAudit = userAudit; }
    public LocalDateTime getAuditDate() { return auditDate; }
    public void setAuditDate(LocalDateTime auditDate) { this.auditDate = auditDate; }
}