package com.saas.systemservice.infrastructure.adapters.controllers.menu;


import com.saas.systemservice.application.dto.request.menu.CreateRoleMenuRequest;
import com.saas.systemservice.application.mappers.menu.RoleMenuApplicationMapper;
import com.saas.systemservice.domain.model.menu.RoleMenu;
import com.saas.systemservice.domain.ports.in.menu.IRoleMenuUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/role-menus")
@RequiredArgsConstructor
public class RoleMenuController {

    private final IRoleMenuUseCase roleMenuUseCase;
    private final RoleMenuApplicationMapper mapper;

    @PostMapping
    public ResponseEntity<RoleMenu> assignMenu(@RequestBody @Validated CreateRoleMenuRequest request) {
        RoleMenu roleMenu = mapper.toDomain(request);
        return new ResponseEntity<>(roleMenuUseCase.create(roleMenu), HttpStatus.CREATED);
    }

    @GetMapping("/role/{roleCode}")
    public ResponseEntity<List<RoleMenu>> getByRole(@PathVariable String roleCode) {
        return ResponseEntity.ok(roleMenuUseCase.getByRoleCode(roleCode));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeAssignment(@PathVariable String id) {
        roleMenuUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}