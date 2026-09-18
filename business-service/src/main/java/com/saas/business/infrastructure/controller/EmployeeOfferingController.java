package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.EmployeeOfferingRequest;
import com.saas.business.application.dto.response.EmployeeOfferingResponse;
import com.saas.business.application.mapper.EmployeeOfferingMapper;
import com.saas.business.domain.port.in.IEmployeeOfferingUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Que servicios presta cada empleado.
 *
 * <p>Se edita entero: la pantalla es una lista de casillas y manda el estado
 * final. Un PUT con la lista completa evita que el front tenga que calcular
 * altas y bajas, que es donde se cuelan los desincronizados.</p>
 */
@RestController
@RequestMapping("/employees/{employeeId}/offerings")
@RequiredArgsConstructor
public class EmployeeOfferingController {

    private final IEmployeeOfferingUseCase useCase;
    private final EmployeeOfferingMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeOfferingResponse>>> byEmployee(
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponseList(useCase.findByEmployee(employeeId))));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<List<EmployeeOfferingResponse>>> replace(
            @PathVariable UUID employeeId,
            @Valid @RequestBody List<EmployeeOfferingRequest> body) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(
                useCase.replaceForEmployee(employeeId, mapper.toDomainList(body))),
                "Servicios del empleado actualizados"));
    }
}
