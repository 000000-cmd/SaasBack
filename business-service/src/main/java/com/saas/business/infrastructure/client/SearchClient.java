package com.saas.business.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign a search-service para lecturas de VISUALIZACIÓN desde el read model de
 * Elasticsearch (nombre + foto de personas en listados). Las escrituras y las
 * lecturas criticas siguen contra thirdparty-service (fuente de verdad).
 * Solo endpoints {@code /internal/**} (S2S). {@code path} = context-path de search.
 */
@FeignClient(name = "search-service", contextId = "search-internal-business", path = "/search")
public interface SearchClient {

    @PostMapping("/internal/third-parties/cards")
    Map<String, PersonCard> personCards(@RequestBody List<String> ids);

    /** Tercero vinculado a una cuenta. 404 si el read model aun no lo proyecto. */
    @GetMapping("/internal/third-parties/by-user/{userId}")
    PersonDoc personByUser(@PathVariable("userId") UUID userId);

    record PersonCard(String fullName, String photoUrl) {}

    /** Solo se necesita el id del tercero (el documento de ES trae mucho mas). */
    record PersonDoc(UUID id) {}
}
