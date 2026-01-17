package com.saas.auth.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Modelo de dominio para Usuario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends BaseDomain {

    private String id;
    private String username;
    private String password;
    private String email;
    private String cellular;
    private String attachment;

    /**
     * Códigos de roles asignados al usuario.
     * Los roles se gestionan en system-service, aquí solo almacenamos los códigos.
     */
    @Builder.Default
    private Set<String> roleCodes = new HashSet<>();

    /**
     * Verifica si el usuario tiene un rol específico.
     */
    public boolean hasRole(String roleCode) {
        return roleCodes != null && roleCodes.contains(roleCode);
    }

    /**
     * Agrega un rol al usuario.
     */
    public void addRole(String roleCode) {
        if (this.roleCodes == null) {
            this.roleCodes = new HashSet<>();
        }
        this.roleCodes.add(roleCode);
    }

    /**
     * Remueve un rol del usuario.
     */
    public void removeRole(String roleCode) {
        if (this.roleCodes != null) {
            this.roleCodes.remove(roleCode);
        }
    }
}