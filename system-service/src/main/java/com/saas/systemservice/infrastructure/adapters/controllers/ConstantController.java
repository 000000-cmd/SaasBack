package com.saas.systemservice.infrastructure.adapters.controllers;

import com.saas.systemservice.application.dto.request.CreateConstantRequest;
import com.saas.systemservice.application.mappers.ConstantApplicationMapper;
import com.saas.systemservice.domain.model.Constant;
import com.saas.systemservice.domain.ports.in.IConstantUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/constants")
@RequiredArgsConstructor
public class ConstantController {

    private final IConstantUseCase constantUseCase;
    private final ConstantApplicationMapper constantMapper;

    @PostMapping
    public ResponseEntity<Constant> createConstant(@RequestBody @Validated CreateConstantRequest constantRequest) {
        Constant constantModel = constantMapper.toDomain(constantRequest);
        Constant createdConstant = constantUseCase.create(constantModel);
        return new ResponseEntity<>(createdConstant, HttpStatus.CREATED);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Constant> getConstantByCode(@PathVariable String code) {
        Constant constant = constantUseCase.getByCode(code);
        return ResponseEntity.ok(constant);
    }

    @PutMapping("/{code}")
    public ResponseEntity<Constant> updateConstant(@PathVariable String code,
                                                   @RequestBody CreateConstantRequest request) {

        Constant constantModel = constantMapper.toDomain(request);
        constantModel.setCode(code);
        Constant updatedConstant = constantUseCase.update(constantModel);
        return ResponseEntity.ok(updatedConstant);
    }

    @PatchMapping("/{code}/status")
    public ResponseEntity<Void> toggleEnabled(@PathVariable String code,
                                              @RequestParam boolean enabled) {
        Constant constant = constantUseCase.getByCode(code);
        constantUseCase.toggleEnabled(constant.getId(), enabled);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteConstant(@PathVariable String code) {
        Constant constant = constantUseCase.getByCode(code);
        constantUseCase.delete(constant.getId());
        return ResponseEntity.noContent().build();
    }
}
