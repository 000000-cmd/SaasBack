package com.saas.authservice.mappers; // Ajusta tu paquete

import com.saas.authservice.dto.internal.ThirdPartyBasicInfoDTO;
import com.saas.authservice.dto.request.user.EmbeddedUserRequestDTO;
import com.saas.authservice.dto.response.LoginResponseDTO;
import com.saas.authservice.dto.response.UserResponseDTO;
import com.saas.authservice.entities.User;
import com.saas.authservice.entities.UserRole;
import com.saas.authservice.models.UserModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // --- Mapeos entre DTO de Petición, Modelo y Entidad ---

    UserModel requestToModel(EmbeddedUserRequestDTO dto);

    @Mapping(target = "attachment", ignore = true) // Ignora la entidad Attachment (será URL)
    @Mapping(target = "roles", ignore = true) // Los roles se asignan manualmente en el servicio
    @Mapping(target = "userAudit", ignore = true) // Auditoría se asigna en el servicio
    @Mapping(target = "auditDate", ignore = true) // Se asigna en la entidad o servicio
    User modelToEntity(UserModel model);

    @Mapping(target = "attachment", ignore = true) // No exponer datos binarios en el modelo
    @Mapping(target = "password", ignore = true) // No exponer password en el modelo
    UserModel entityToModel(User entity);

    // --- Mapeos a DTOs de Respuesta ---

    UserResponseDTO modelToResponseDTO(UserModel model);

    // Método inverso útil (si necesitas crear una entidad User desde un UserResponseDTO)
    default User responseToEntity(UserResponseDTO dto) {
        if (dto == null) return null;
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setCellular(dto.getCellular());
        // Otros campos como password, roles, auditoría, etc.,
        // NO se mapean desde un DTO de respuesta.
        return user;
    }


    /**
     * Convierte una entidad User y la información básica de ThirdParty (obtenida externamente)
     * al DTO de respuesta del Login.
     * @param user La entidad User autenticada.
     * @param thirdPartyInfo DTO con firstName y firstLastName (puede ser null).
     * @return El LoginResponseDTO base (sin accessToken).
     */
    @Mapping(target = "accessToken", ignore = true) // El token se añade en el AuthService
    @Mapping(target = "name", expression = "java(determineDisplayNameHelper(user, thirdPartyInfo))")
    @Mapping(target = "roles", source = "user.roles", qualifiedByName = "mapRolesToStrings")
    LoginResponseDTO toLoginResponseDTO(User user, ThirdPartyBasicInfoDTO thirdPartyInfo);


    // --- Métodos Helper para Mapeos Complejos ---

    default String determineDisplayNameHelper(User user, ThirdPartyBasicInfoDTO thirdPartyInfo) {
        if (thirdPartyInfo != null && thirdPartyInfo.getFirstName() != null && !thirdPartyInfo.getFirstName().isEmpty()) {
            return thirdPartyInfo.getFirstName() + (thirdPartyInfo.getFirstLastName() != null ? " " + thirdPartyInfo.getFirstLastName() : "");
        }
        return (user != null) ? user.getUsername() : "Usuario Desconocido";
    }

    @Named("mapRolesToStrings")
    default List<String> mapRolesToStrings(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                // Asegúrate que getRole() y getCode() no sean nulos antes de llamar
                .filter(userRole -> userRole.getRole() != null && userRole.getRole().getCode() != null)
                .map(userRole -> userRole.getRole().getCode())
                .collect(Collectors.toList());
    }

    // MapStruct necesita a veces un método para acceder al parámetro 'user' dentro de los helpers
    default User getUserFromContext(User user) {
        return user;
    }
}


