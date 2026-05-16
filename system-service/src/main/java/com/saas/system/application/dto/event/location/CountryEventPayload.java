package com.saas.system.application.dto.event.location;

import com.saas.system.domain.model.location.Country;

import java.util.UUID;

public record CountryEventPayload(
        UUID id,
        String code,
        String name,
        String officialName,
        String isoCode3,
        String numericCode,
        String phoneCode,
        String currencyCode,
        String currencySymbol,
        String continent,
        Boolean enabled
) {
    public static CountryEventPayload from(Country c) {
        return new CountryEventPayload(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getOfficialName(),
                c.getIsoCode3(),
                c.getNumericCode(),
                c.getPhoneCode(),
                c.getCurrencyCode(),
                c.getCurrencySymbol(),
                c.getContinent(),
                c.getEnabled()
        );
    }
}
