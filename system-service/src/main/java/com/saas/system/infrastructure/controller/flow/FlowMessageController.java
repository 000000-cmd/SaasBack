package com.saas.system.infrastructure.controller.flow;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.flow.FlowMessageRequest;
import com.saas.system.application.dto.response.flow.FlowMessageView;
import com.saas.system.application.service.flow.FlowMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Los textos de los flujos. El front los consume POR CODIGO.
 */
@RestController
@RequestMapping("/flow-messages")
@RequiredArgsConstructor
public class FlowMessageController {

    private final FlowMessageService service;

    /**
     * Todos los activos de golpe, del codigo al texto.
     *
     * <p>Es lo que pide una pantalla al abrirse. Pedirlos de uno en uno serian
     * diez viajes para diez frases.</p>
     */
    @GetMapping("/map")
    public ResponseEntity<ApiResponse<Map<String, String>>> map() {
        return ResponseEntity.ok(ApiResponse.success(service.byCode()));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<FlowMessageView>> byCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(service.getByCode(code)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FlowMessageView>>> list() {
        return ResponseEntity.ok(ApiResponse.success(service.list()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowMessageView>> create(
            @Valid @RequestBody FlowMessageRequest req) {
        return ResponseEntity.ok(ApiResponse.created(service.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowMessageView>> update(
            @PathVariable UUID id, @Valid @RequestBody FlowMessageRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req), "Texto guardado"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Texto eliminado"));
    }
}
