package com.saas.auth.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad JPA para Usuario.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "auth_users")
@EqualsAndHashCode(callSuper = true, exclude = {"roleCodes"})
public class UserEntity extends BaseEntity {

    @Id
    @Column(name = "Id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "Username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "Password", nullable = false)
    private String password;

    @Column(name = "Email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "Cellular", length = 20)
    private String cellular;

    @Column(name = "Attachment")
    private String attachment;

    /**
     * Roles del usuario.
     * Se almacenan como una tabla de unión con los códigos de rol.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "auth_userroles",
            joinColumns = @JoinColumn(name = "UserId")
    )
    @Column(name = "RoleCode")
    @Builder.Default
    private Set<String> roleCodes = new HashSet<>();

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
