package com.saas.thirdparty.infrastructure.controller;

import com.saas.thirdparty.application.dto.event.ThirdPartyEventPayload;
import com.saas.thirdparty.application.dto.request.ThirdPartyRequest;
import com.saas.thirdparty.application.dto.response.ThirdPartyResponse;
import com.saas.thirdparty.application.mapper.ThirdPartyMapper;
import com.saas.thirdparty.application.service.ThirdPartyReindexPublisher;
import com.saas.thirdparty.domain.model.ThirdParty;
import com.saas.thirdparty.domain.port.in.IThirdPartyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Endpoints internos S2S (sin JWT; {@code /internal/**} permitido en SecurityConfig).
 * Consumidos por otros microservicios via Feign:
 *  - search-service: reindex-from-source (payload del tercero).
 *  - business-service: alta de la persona durante el aprovisionamiento del negocio.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final ThirdPartyReindexPublisher reindexPublisher;
    private final IThirdPartyUseCase useCase;
    private final ThirdPartyMapper mapper;

    @GetMapping("/third-parties/all")
    public List<ThirdPartyEventPayload> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        return reindexPublisher.buildPage(page, size);
    }

    @GetMapping("/third-parties/count")
    public Map<String, Long> count() {
        return Map.of("total", useCase.count());
    }

    /** Alta S2S de una persona (usada por el aprovisionamiento del negocio). */
    @PostMapping("/third-parties")
    public ThirdPartyResponse create(@Valid @RequestBody ThirdPartyRequest req) {
        return mapper.toResponse(useCase.create(mapper.toDomain(req)));
    }

    /** Nombres de personas en lote (id -> nombre completo). Para listas detalladas S2S. */
    @PostMapping("/third-parties/names")
    public Map<String, String> names(@RequestBody Set<UUID> ids) {
        Map<String, String> out = new HashMap<>();
        for (ThirdParty t : useCase.findByIds(ids)) {
            out.put(t.getId().toString(), fullName(t));
        }
        return out;
    }

    private static String fullName(ThirdParty t) {
        return String.join(" ", Stream.of(t.getFirstName(), t.getSecondName(), t.getFirstLastName(), t.getSecondLastName())
                .filter(s -> s != null && !s.isBlank()).toList());
    }

    /** Resuelve la persona vinculada a una cuenta (para "mi empresa"). 404 si no existe. */
    @GetMapping("/third-parties/by-user/{userId}")
    public ResponseEntity<ThirdPartyResponse> byUser(@PathVariable UUID userId) {
        return useCase.findByUserId(userId)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
