package com.saas.authservice.entities;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "userroles")
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "IdUserRole") // Correcto, sigue la regla Id<Tabla>
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdUser", referencedColumnName = "IdUser") // Correcto, apunta a IdUser
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdList", referencedColumnName = "IdList") // Corregido para apuntar a IdList
    private ListRole role;

    // Constructores, Getters y Setters (sin cambios)
    public UserRole() {}

    public UserRole(User user, ListRole role) {
        this.user = user;
        this.role = role;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public ListRole getRole() { return role; }
    public void setRole(ListRole role) { this.role = role; }
}