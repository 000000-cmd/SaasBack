package com.saas.system.application.dto.event.location;

import com.saas.system.domain.model.location.Country;
import com.saas.system.domain.model.location.Department;
import com.saas.system.domain.model.location.Municipality;

import java.util.UUID;

public record MunicipalityEventPayload(
        UUID id,
        String code,
        String name,
        UUID departmentId,
        String departmentCode,
        String departmentName,
        UUID countryId,
        String countryCode,
        String countryName,
        Boolean enabled
) {
    public static MunicipalityEventPayload from(Municipality m, Department dept, Country country) {
        return new MunicipalityEventPayload(
                m.getId(),
                m.getCode(),
                m.getName(),
                m.getDepartmentId(),
                dept != null ? dept.getCode() : null,
                dept != null ? dept.getName() : null,
                country != null ? country.getId() : null,
                country != null ? country.getCode() : null,
                country != null ? country.getName() : null,
                m.getEnabled()
        );
    }
}
