package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BusinessClientRequest;
import com.saas.business.application.dto.response.BusinessClientResponse;
import com.saas.business.application.mapper.BusinessClientMapper;
import com.saas.business.domain.model.BusinessClient;
import com.saas.business.domain.port.in.IBusinessClientUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Los clientes de un negocio.
 *
 * <p>NO existe un listado global de clientes, y esa ausencia es la
 * funcionalidad: la antigua tabla {@code client} tenia un {@code GET /clients}
 * que devolvia a todo el mundo sin acotar por negocio. Aqui toda lectura pasa
 * por un negocio, siempre.</p>
 *
 * <p>De donde sale ese negocio es el siguiente paso: hoy llega por parametro,
 * como en el resto del sistema, y eso significa que un usuario autenticado
 * puede pedir el de otro cambiando un identificador. Se cierra derivandolo del
 * token para TODOS los servicios a la vez; hacerlo solo aqui dejaria dos
 * patrones conviviendo y el agujero abierto en los demas.</p>
 */
@RestController
@RequestMapping("/business-clients")
@RequiredArgsConstructor
public class BusinessClientController {

    private final IBusinessClientUseCase useCase;
    private final BusinessClientMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessClientResponse>>> byBusiness(
            @RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponseList(useCase.byBusiness(businessId))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessClientResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }

    /**
     * Busca por telefono dentro del negocio. El telefono llega escrito como
     * sea; se normaliza antes de buscar.
     */
    @GetMapping("/by-phone")
    public ResponseEntity<ApiResponse<BusinessClientResponse>> byPhone(
            @RequestParam UUID businessId,
            @RequestParam String phone) {
        return useCase.findByPhone(businessId, phone)
                .map(c -> ResponseEntity.ok(ApiResponse.success(mapper.toResponse(c))))
                .orElseGet(() -> ResponseEntity.ok(
                        ApiResponse.<BusinessClientResponse>builder()
                                .success(true)
                                .message("Sin cliente con ese teléfono")
                                .status(200)
                                .build()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessClientResponse>> create(
            @Valid @RequestBody BusinessClientRequest req) {
        BusinessClient creado = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessClientResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody BusinessClientRequest req) {
        BusinessClient existente = useCase.getById(id);
        mapper.updateDomain(req, existente);
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponse(useCase.update(id, existente))));
    }

    /**
     * Soft-delete. Nunca fisico: un cliente borrado de verdad se llevaria por
     * delante el historial de sus citas.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Cliente deshabilitado"));
    }
}
