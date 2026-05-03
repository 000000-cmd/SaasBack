package com.saas.auth.application.dto.event;

import com.saas.auth.domain.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 * Payload del evento {@code user.*}. Estructura plana, libre de entidades JPA
 *
 * no se debe incluir passwordHash, refresh tokens ni secretos: el evento
 * llega a multiples consumers (search, audit, futuro notifications) y todo se publica
 */
@Data
@Builder
public class UserEventPayload {

    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Boolean enabled;
    private Set<String> roleCodes;

    /** Construye el payload desde un User de dominio */
    public static UserEventPayload from(User u) {
        return UserEventPayload.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .fullName(u.getFullName())
                .enabled(u.getEnabled())
                .roleCodes(u.getRoleCodes())
                .build();
    }

}
