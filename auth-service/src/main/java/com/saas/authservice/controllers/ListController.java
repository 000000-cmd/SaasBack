package com.saas.authservice.controllers; // Ajusta tu paquete

import com.saas.authservice.dto.request.ListTypeRequestDTO;
import com.saas.authservice.dto.response.ListTypeResponseDTO;
import com.saas.authservice.services.AbstractListService;
import com.saas.authservice.services.ListRoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lists") // Mantenemos el prefijo /api/lists
public class ListController {

    // --- SÓLO SE INYECTA EL SERVICIO DE ROLES ---
    private final ListRoleService roleTypeService;

    // --- ELIMINADOS: Todos los demás servicios de listas ---

    @Autowired // Buena práctica
    public ListController(ListRoleService roleTypeService) {
        this.roleTypeService = roleTypeService;
    }

    // --- Métodos Helper Genéricos (Opcional, podrías moverlos a AbstractListService si lo refactorizas) ---
    private ResponseEntity<List<ListTypeResponseDTO>> getAll(AbstractListService<?, ?, ?> service) {
        return ResponseEntity.ok(service.findAll());
    }
    private ResponseEntity<ListTypeResponseDTO> getById(AbstractListService<?, ?, ?> service, UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }
    private ResponseEntity<ListTypeResponseDTO> create(AbstractListService<?, ?, ?> service, ListTypeRequestDTO dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }
    private ResponseEntity<ListTypeResponseDTO> update(AbstractListService<?, ?, ?> service, UUID id, ListTypeRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
    private ResponseEntity<Void> delete(AbstractListService<?, ?, ?> service, UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- CRUD SÓLO para Role Types (/api/lists/roletypes) ---
    @GetMapping("/roletypes")
    public ResponseEntity<List<ListTypeResponseDTO>> getAllRoleTypes() {
        return getAll(roleTypeService);
    }

    @GetMapping("/roletypes/{id}")
    public ResponseEntity<ListTypeResponseDTO> getRoleTypeById(@PathVariable UUID id) {
        return getById(roleTypeService, id);
    }

    @PostMapping("/roletypes")
    public ResponseEntity<ListTypeResponseDTO> createRoleType(@Valid @RequestBody ListTypeRequestDTO dto) {
        return create(roleTypeService, dto);
    }

    @PutMapping("/roletypes/{id}")
    public ResponseEntity<ListTypeResponseDTO> updateRoleType(@PathVariable UUID id, @Valid @RequestBody ListTypeRequestDTO dto) {
        return update(roleTypeService, id, dto);
    }

    @DeleteMapping("/roletypes/{id}")
    public ResponseEntity<Void> deleteRoleType(@PathVariable UUID id) {
        return delete(roleTypeService, id);
    }

}
