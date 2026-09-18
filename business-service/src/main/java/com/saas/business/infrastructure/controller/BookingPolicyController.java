package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BookingPolicyRequest;
import com.saas.business.application.dto.response.BookingPolicyResponse;
import com.saas.business.application.mapper.BookingPolicyMapper;
import com.saas.business.domain.port.in.IBusinessBookingPolicyUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * La configuracion de reservas del negocio.
 *
 * <p>Un GET siempre responde algo: si el negocio nunca la toco, devuelve los
 * valores por defecto sin crear la fila. La pantalla no tiene que distinguir
 * entre "sin configurar" y "configurado igual que por defecto".</p>
 */
@RestController
@RequestMapping("/booking-policies")
@RequiredArgsConstructor
public class BookingPolicyController {

    private final IBusinessBookingPolicyUseCase useCase;
    private final BookingPolicyMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<BookingPolicyResponse>> forBusiness(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponse(useCase.forBusiness(businessId))));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<BookingPolicyResponse>> save(
            @RequestParam UUID businessId,
            @Valid @RequestBody BookingPolicyRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponse(useCase.saveFor(businessId, mapper.toDomain(req))),
                "Configuración de reservas guardada"));
    }
}
