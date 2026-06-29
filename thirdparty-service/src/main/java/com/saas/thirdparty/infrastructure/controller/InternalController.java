package com.saas.thirdparty.infrastructure.controller;

import com.saas.thirdparty.application.dto.event.ThirdPartyEventPayload;
import com.saas.thirdparty.application.dto.request.ThirdPartyRequest;
import com.saas.thirdparty.application.dto.response.ThirdPartyResponse;
import com.saas.thirdparty.application.mapper.ThirdPartyMapper;
import com.saas.thirdparty.application.service.ThirdPartyReindexPublisher;
import com.saas.thirdparty.domain.port.in.IThirdPartyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
}
