package com.saas.system.application.dto.event.location;

import com.saas.system.domain.model.location.Country;
import com.saas.system.domain.model.location.Department;

import java.util.UUID;

public record DepartmentEventPayload(
        UUID id,
        String code,
        String name,
        UUID countryId,
        String countryCode,
        String countryName,
        Boolean enabled
) {
    public static DepartmentEventPayload from(Department d, Country country) {
        return new DepartmentEventPayload(
                d.getId(),
                d.getCode(),
                d.getName(),
                d.getCountryId(),
                country != null ? country.getCode() : null,
                country != null ? country.getName() : null,
                d.getEnabled()
        );
    }
}
