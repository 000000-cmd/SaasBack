package com.saas.events.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.events.application.service.VerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Verificacion de un contacto por codigo.
 *
 * Dos endpoints y nada mas: pedir y comprobar. El canal por el que sale el
 * codigo NO se elige aqui — lo decide el tipo del contacto — precisamente para
 * que anadir SMS o WhatsApp no obligue a tocar esta interfaz.
 */
@RestController
@RequestMapping("/notification/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verification;

    public record RequestBodyDto(@NotNull UUID contactId) {}
    public record VerifyBodyDto(@NotNull UUID contactId, @NotBlank String code) {}

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<VerificationService.Requested>> request(
            @Valid @RequestBody RequestBodyDto body) {
        return ResponseEntity.ok(ApiResponse.success(verification.request(body.contactId())));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verify(
            @Valid @RequestBody VerifyBodyDto body) {
        boolean ok = verification.verify(body.contactId(), body.code());
        return ResponseEntity.ok(ApiResponse.success(Map.of("verified", ok)));
    }
}
