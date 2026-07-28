package com.saas.search.application.dto.search;

/**
 * Cuerpo (POST) de una busqueda de localizacion. Reemplaza los seis endpoints
 * GET que habia por nivel: el nivel viaja como un campo mas, asi el front tiene
 * UNA ruta que llenar en vez de armar querystrings distintos por nivel.
 *
 * <pre>
 *   { "level": "MUNICIPIO", "q": "med", "country": "CO", "department": "05" }
 * </pre>
 *
 * {@code level} acepta PAIS | DEPARTAMENTO | MUNICIPIO | BARRIO | VEREDA |
 * CORREGIMIENTO | OTRO | NEIGHBORHOOD (este ultimo = cualquier tipo de barrio).
 */
public record LocationSearchRequest(
        String level,
        String q,
        String country,
        String department,
        String municipality,
        Integer page,
        Integer size
) {
    public int pageOrDefault() { return page == null || page < 0 ? 0 : page; }
    public int sizeOrDefault() { return size == null || size <= 0 ? 20 : size; }
}
