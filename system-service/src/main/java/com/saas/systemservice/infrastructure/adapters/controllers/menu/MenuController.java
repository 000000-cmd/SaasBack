package com.saas.systemservice.infrastructure.adapters.controllers.menu;

import com.saas.systemservice.application.dto.request.menu.CreateMenuRequest;
import com.saas.systemservice.application.mappers.menu.MenuApplicationMapper;
import com.saas.systemservice.domain.model.menu.Menu;
import com.saas.systemservice.domain.ports.in.menu.IMenuUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class MenuController {

    private final IMenuUseCase menuUseCase;
    private final MenuApplicationMapper mapper;

    @PostMapping
    public ResponseEntity<Menu> create(@RequestBody @Validated CreateMenuRequest request) {
        return new ResponseEntity<>(menuUseCase.create(mapper.toDomain(request)), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Menu>> getAll() {
        return ResponseEntity.ok(menuUseCase.getAll());
    }

    @GetMapping("/{code}")
    public ResponseEntity<Menu> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(menuUseCase.getByCode(code));
    }

    @PutMapping("/{code}")
    public ResponseEntity<Menu> update(@PathVariable String code,
                                       @RequestBody @Validated CreateMenuRequest request) {
        Menu menuModel = mapper.toDomain(request);
        menuModel.setCode(code);
        Menu updated = menuUseCase.update(menuModel);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        Menu menu = menuUseCase.getByCode(code);
        menuUseCase.delete(menu.getId());
        return ResponseEntity.noContent().build();
    }
}