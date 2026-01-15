package com.saas.authservice.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users") // Asumo el nombre de la tabla "users"
public class User {

    @Id
    @Column(name = "IdUser") // Corregido
    private UUID id;

    @Column(name = "Username", nullable = false, unique = true)
    private String username;

    @Column(name = "Password", nullable = false)
    private String password;

    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @Column(name = "Cellular")
    private String cellular;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdUserAudit", referencedColumnName = "IdUser") // Corregido
    private User userAudit;

    @Column(name = "AuditDate", nullable = false)
    private LocalDateTime auditDate = LocalDateTime.now();

    @Column(name = "Attachment")
    private String attachment;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<UserRole> roles;

    public User() {
    }

    public User(UUID id, String username, String password, String email, String cellular, User userAudit, LocalDateTime auditDate, String attachment, Set<UserRole> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.cellular = cellular;
        this.userAudit = userAudit;
        this.auditDate = auditDate;
        this.attachment = attachment;
        this.roles = roles;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCellular() {
        return cellular;
    }

    public void setCellular(String cellular) {
        this.cellular = cellular;
    }

    public User getUserAudit() {
        return userAudit;
    }

    public void setUserAudit(User userAudit) {
        this.userAudit = userAudit;
    }

    public LocalDateTime getAuditDate() {
        return auditDate;
    }

    public void setAuditDate(LocalDateTime auditDate) {
        this.auditDate = auditDate;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<UserRole> roles) {
        this.roles = roles;
    }
}