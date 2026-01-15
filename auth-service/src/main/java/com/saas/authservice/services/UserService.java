package com.saas.authservice.services; // Asegúrate de que este sea tu paquete de servicios

import com.saas.authservice.dto.request.user.EmbeddedUserRequestDTO;
import com.saas.authservice.dto.response.UserResponseDTO;
import com.saas.authservice.entities.ListRole;
import com.saas.authservice.entities.User;
import com.saas.authservice.entities.UserRole;
import com.saas.authservice.exceptions.DuplicateResourceException;
import com.saas.authservice.exceptions.ResourceNotFoundException;
import com.saas.authservice.mappers.UserMapper;
import com.saas.authservice.models.UserModel;
import com.saas.authservice.repositories.ListRoleRepository;
import com.saas.authservice.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j // Para logging
@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder; // Inyecta la interfaz
    private final UserMapper userMapper;
    private final ListRoleRepository roleRepo;

    @Autowired // Buena práctica añadir @Autowired en el constructor
    public UserService(UserRepository userRepo,
                       UserMapper userMapper,
                       ListRoleRepository roleRepo,
                       PasswordEncoder passwordEncoder) { // Inyecta PasswordEncoder
        this.userRepo = userRepo;
        this.userMapper = userMapper;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder; // Asigna el inyectado
    }

    @Transactional // Asegura que toda la operación sea atómica
    public UserResponseDTO createUser(EmbeddedUserRequestDTO userRequestDTO) {
        log.info("Intentando crear usuario con username: {}", userRequestDTO.getUsername());

        // 1. Validar duplicados de username y email
        if (userRepo.existsByUsername(userRequestDTO.getUsername())) {
            log.warn("Intento de crear usuario con username duplicado: {}", userRequestDTO.getUsername());
            throw new DuplicateResourceException("El nombre de usuario '" + userRequestDTO.getUsername() + "' ya está en uso.");
        }
        if (userRepo.existsByEmail(userRequestDTO.getEmail())) {
            log.warn("Intento de crear usuario con email duplicado: {}", userRequestDTO.getEmail());
            throw new DuplicateResourceException("El correo electrónico '" + userRequestDTO.getEmail() + "' ya está registrado.");
        }

        // 2. Mapear DTO a Modelo y luego a Entidad (generando ID)
        UserModel userModel = userMapper.requestToModel(userRequestDTO);
        userModel.setId(UUID.randomUUID()); // Genera el ID aquí
        User userEntity = userMapper.modelToEntity(userModel);

        // 3. Procesar y asignar roles si vienen en el DTO
        Set<UserRole> assignedRoles = new HashSet<>();
        if (userRequestDTO.getRoleCodes() != null && !userRequestDTO.getRoleCodes().isEmpty()) {
            log.debug("Asignando roles: {}", userRequestDTO.getRoleCodes());
            for (String roleCode : userRequestDTO.getRoleCodes()) {
                ListRole roleType = roleRepo.findByCode(roleCode)
                        .orElseThrow(() -> {
                            log.error("Rol no encontrado con código: {}", roleCode);
                            return new ResourceNotFoundException("El rol con código '" + roleCode + "' no existe.");
                        });
                // Importante: Asegúrate que el constructor de UserRole maneje la relación bidireccional si es necesario
                assignedRoles.add(new UserRole(userEntity, roleType));
            }
        } else {
            log.warn("No se especificaron roles para el nuevo usuario: {}", userRequestDTO.getUsername());
            // Considera asignar un rol por defecto si es necesario, ej. "USER"
            // ListRole defaultRole = roleRepo.findByCode("USER").orElseThrow(...);
            // assignedRoles.add(new UserRole(userEntity, defaultRole));
        }
        userEntity.setRoles(assignedRoles); // Asigna el conjunto de roles

        // 4. Codificar la contraseña
        userEntity.setPassword(passwordEncoder.encode(userRequestDTO.getPassword())); // Codifica la contraseña del DTO

        // 5. Establecer campos de auditoría inicial
        userEntity.setUserAudit(userEntity); // Para la creación, el usuario es su propio auditor
        userEntity.setAuditDate(LocalDateTime.now()); // Establece la fecha de creación/auditoría

        // 6. Guardar la entidad en la base de datos
        log.debug("Guardando nueva entidad User...");
        User savedUserEntity = userRepo.save(userEntity);
        log.info("Usuario creado exitosamente con ID: {}", savedUserEntity.getId());

        // 7. Mapear la entidad guardada de vuelta a Modelo y luego a DTO de Respuesta
        UserModel responseModel = userMapper.entityToModel(savedUserEntity);
        return userMapper.modelToResponseDTO(responseModel);
    }
}
