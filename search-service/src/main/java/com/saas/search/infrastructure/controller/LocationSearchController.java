package com.saas.search.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.search.application.dto.search.SearchResponse;
import com.saas.search.application.service.search.LocationSearchService;
import com.saas.search.domain.document.LocationDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de busqueda jerarquica de localizacion.
 *
 * <p>Todos los endpoints de busqueda full-text requieren minimo 3 caracteres en {@code q}.
 * Si {@code q} tiene menos de 3 caracteres se retorna lista vacia.</p>
 *
 * <p>Cada resultado lleva la jerarquia completa ya desnormalizada:
 * {@code countryCode/Name}, {@code departmentCode/Name}, {@code municipalityCode/Name},
 * {@code neighborhoodCode/Name + Type}, {@code fullPath} y {@code searchText}.</p>
 */
@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationSearchController {

    private final LocationSearchService searchService;

    /** Paises por nombre (>= 3 letras). */
    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<SearchResponse<LocationDocument>>> countries(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(searchService.searchCountries(q, page, size)));
    }

    /** Departamentos por nombre (>= 3 letras), opcional filtro por pais (code ISO). */
    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<SearchResponse<LocationDocument>>> departments(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                searchService.searchDepartments(q, country, page, size)));
    }

    /** Municipios por nombre (>= 3 letras), filtros opcionales por country/department. */
    @GetMapping("/municipalities")
    public ResponseEntity<ApiResponse<SearchResponse<LocationDocument>>> municipalities(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                searchService.searchMunicipalities(q, country, department, page, size)));
    }

    /**
     * Barrios/veredas por nombre. Devuelve jerarquia completa por hit.
     * Filtros opcionales: country, department, municipality, type (BARRIO|VEREDA|CORREGIMIENTO|OTRO).
     */
    @GetMapping("/neighborhoods")
    public ResponseEntity<ApiResponse<SearchResponse<LocationDocument>>> neighborhoods(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String municipality,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                searchService.searchNeighborhoods(q, country, department, municipality, type, page, size)));
    }

    /**
     * Buscar UNICAMENTE por barrio/vereda (>= 3 letras) y devolver toda la cadena padre.
     * Ideal para selects de autocompletado donde el usuario escribe el barrio.
     */
    @GetMapping("/by-neighborhood")
    public ResponseEntity<ApiResponse<SearchResponse<LocationDocument>>> byNeighborhood(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(searchService.searchByNeighborhood(q, page, size)));
    }

    /**
     * Busqueda en cascada. Especifica padres conocidos + nivel objetivo + 3+ letras.
     * <pre>
     *   level=PAIS,         q=col                           -> Colombia
     *   level=DEPARTAMENTO, q=ant, country=CO               -> Antioquia
     *   level=MUNICIPIO,    q=med, country=CO, department=05 -> Medellin
     *   level=BARRIO,       q=pob, country=CO, department=05, municipality=05001 -> El Poblado
     * </pre>
     */
    @GetMapping("/cascade")
    public ResponseEntity<ApiResponse<SearchResponse<LocationDocument>>> cascade(
            @RequestParam String level,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String municipality,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                searchService.cascade(level, q, country, department, municipality, page, size)));
    }
}
