package com.saas.system.application.dto.event.location;

import java.util.UUID;

/**
 * DTO denormalizado para reindex de location hacia search-service.
 *
 * <p>Los nombres de campo coinciden con {@code LocationDocument} de
 * search-service para que Jackson los mapee directo sin transformaciones
 * adicionales en el pipeline de reindex generico.
 *
 * <p>Un solo record cubre los 4 niveles; los campos del nivel propio
 * y de los padres se llenan segun el nivel:
 * <ul>
 *   <li>PAIS:        countryId/Code/Name</li>
 *   <li>DEPARTAMENTO: country* + department*</li>
 *   <li>MUNICIPIO:   country* + department* + municipality*</li>
 *   <li>BARRIO/etc:  country* + department* + municipality* + neighborhood* + neighborhoodType</li>
 * </ul>
 */
public record LocationReindexPayload(
        UUID id,
        String level,
        Boolean enabled,

        UUID countryId,        String countryCode,        String countryName,
        UUID departmentId,     String departmentCode,     String departmentName,
        UUID municipalityId,   String municipalityCode,   String municipalityName,
        UUID neighborhoodId,   String neighborhoodCode,   String neighborhoodName,
        String neighborhoodType,

        String searchText,
        String fullPath
) {}
