package com.saas.search.infrastructure.controller;

import com.saas.search.application.service.search.ThirdPartySearchService;
import com.saas.search.application.service.search.ThirdPartySearchService.PersonCard;
import com.saas.search.domain.document.ThirdPartyDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints internos S2S de búsqueda (sin JWT; {@code /internal/**} permitido).
 * Consumidos por otros microservicios que necesitan leer del read model de ES
 * en sus listados (p. ej. business-service resuelve nombre+foto de empleados).
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalSearchController {

    private final ThirdPartySearchService thirdPartySearch;

    /** Tarjetas de persona (nombre + foto) en lote desde ES. */
    @PostMapping("/third-parties/cards")
    public Map<String, PersonCard> thirdPartyCards(@RequestBody List<String> ids) {
        return thirdPartySearch.cardsByIds(ids);
    }

    /**
     * Tercero vinculado a una cuenta. 404 si el read model aun no lo tiene: el
     * llamador cae a la fuente de verdad (thirdparty-service).
     */
    @GetMapping("/third-parties/by-user/{userId}")
    public ResponseEntity<ThirdPartyDocument> thirdPartyByUser(@PathVariable String userId) {
        return ResponseEntity.ofNullable(thirdPartySearch.findByUserId(userId));
    }

    /** Cuenta duena de un numero de documento (login flexible). 404 si no esta en ES. */
    @GetMapping("/third-parties/user-by-document")
    public ResponseEntity<UserByDocument> userByDocument(@RequestParam String documentNumber) {
        ThirdPartyDocument doc = thirdPartySearch.findByDocumentNumber(documentNumber);
        return doc == null || doc.getUserId() == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(new UserByDocument(doc.getUserId()));
    }

    public record UserByDocument(UUID userId) {}
}
