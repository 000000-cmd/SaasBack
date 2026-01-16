package com.saas.systemservice.infrastructure.adapters.controllers.lists;


import com.saas.systemservice.application.dto.request.CreateRoleRequest;
import com.saas.systemservice.application.mappers.RoleApplicationMapper;
import com.saas.systemservice.domain.model.lists.Role;
import com.saas.systemservice.domain.ports.in.lists.IRoleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleUseCase roleUseCase;
    private final RoleApplicationMapper roleMapper;

    @PostMapping
    public ResponseEntity<Role> create(@RequestBody @Validated CreateRoleRequest request) {
        // Usamos toDomain() gracias a la interfaz genérica
        Role roleModel = roleMapper.toDomain(request);
        Role created = roleUseCase.create(roleModel);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Role>> getAll() {
        return ResponseEntity.ok(roleUseCase.getAll());
    }

    @GetMapping("/{code}")
    public ResponseEntity<Role> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(roleUseCase.getByCode(code));
    }

    @PutMapping("/{code}")
    public ResponseEntity<Role> update(@PathVariable String code,
                                       @RequestBody @Validated CreateRoleRequest request) {
        Role roleModel = roleMapper.toDomain(request);
        roleModel.setCode(code); // Aseguramos que el código de la URL mande
        Role updated = roleUseCase.update(roleModel);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{code}/status")
    public ResponseEntity<Void> toggleStatus(@PathVariable String code,
                                             @RequestParam boolean enabled) {
        // Buscamos el ID primero porque el toggle genérico usa ID (o podrías sobrecargar para usar code)
        Role role = roleUseCase.getByCode(code);
        roleUseCase.toggleEnabled(role.getId(), enabled);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        Role role = roleUseCase.getByCode(code);
        roleUseCase.delete(role.getId());
        return ResponseEntity.noContent().build();
    }
}
