package com.saas.search.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.search.application.dto.search.LocationSearchRequest;
import com.saas.search.application.dto.search.SearchResponse;
import com.saas.search.application.service.search.LocationSearchService;
import com.saas.search.domain.document.LocationDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Busqueda jerarquica de localizacion. UN solo endpoint: el nivel y los padres
 * conocidos viajan en el cuerpo, no en la ruta ni en el querystring.
 *
 * <pre>
 *   POST /search/locations
 *   { "level": "PAIS",         "q": "col" }
 *   { "level": "DEPARTAMENTO", "q": "ant", "country": "CO" }
 *   { "level": "MUNICIPIO",    "q": "med", "country": "CO", "department": "05" }
 *   { "level": "NEIGHBORHOOD", "q": "pob", "municipality": "05001" }
 * </pre>
 *
 * <p>{@code q} exige minimo 3 caracteres en los niveles con nombre; con menos
 * la respuesta va vacia. Cada hit trae la jerarquia completa desnormalizada
 * ({@code countryCode/Name} ... {@code neighborhoodCode/Name}, {@code fullPath}).
 */
@RestController
@RequiredArgsConstructor
public class LocationSearchController {

    private final LocationSearchService searchService;

    // La ruta va completa en el metodo (y no repartida con un @RequestMapping de
    // clase): un @PostMapping sin valor no llega a registrarse y la peticion
    // acaba en el handler de recursos estaticos.
    @PostMapping("/locations")
    public ResponseEntity<ApiResponse<SearchResponse<LocationDocument>>> search(
            @RequestBody LocationSearchRequest req) {

        SearchResponse<LocationDocument> result = searchService.cascade(
                req.level(), req.q(), req.country(), req.department(), req.municipality(),
                req.pageOrDefault(), req.sizeOrDefault());

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
