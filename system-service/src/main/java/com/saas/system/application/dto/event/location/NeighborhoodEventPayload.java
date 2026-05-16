package com.saas.system.application.dto.event.location;

import com.saas.system.domain.model.location.Country;
import com.saas.system.domain.model.location.Department;
import com.saas.system.domain.model.location.Municipality;
import com.saas.system.domain.model.location.Neighborhood;

import java.util.UUID;

public record NeighborhoodEventPayload(
        UUID id,
        String code,
        String name,
        String type,
        UUID municipalityId,
        String municipalityCode,
        String municipalityName,
        UUID departmentId,
        String departmentCode,
        String departmentName,
        UUID countryId,
        String countryCode,
        String countryName,
        Boolean enabled
) {
    public static NeighborhoodEventPayload from(Neighborhood n, Municipality muni, Department dept, Country country) {
        return new NeighborhoodEventPayload(
                n.getId(),
                n.getCode(),
                n.getName(),
                n.getType(),
                n.getMunicipalityId(),
                muni != null ? muni.getCode() : null,
                muni != null ? muni.getName() : null,
                dept != null ? dept.getId() : null,
                dept != null ? dept.getCode() : null,
                dept != null ? dept.getName() : null,
                country != null ? country.getId() : null,
                country != null ? country.getCode() : null,
                country != null ? country.getName() : null,
                n.getEnabled()
        );
    }
}
