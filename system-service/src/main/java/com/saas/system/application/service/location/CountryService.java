package com.saas.system.application.service.location;

import com.saas.common.events.EventTypes;
import com.saas.common.outbox.OutboxPublisher;
import com.saas.common.service.CodeCrudService;
import com.saas.system.application.dto.event.location.CountryEventPayload;
import com.saas.system.domain.model.location.Country;
import com.saas.system.domain.port.out.location.ICountryRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CountryService extends CodeCrudService<Country, UUID> {

    private final OutboxPublisher outbox;

    public CountryService(ICountryRepositoryPort repository, OutboxPublisher outbox) {
        super(repository);
        this.outbox = outbox;
    }

    @Override
    protected String getResourceName() {
        return "Pais";
    }

    @Override
    protected void applyChanges(Country existing, Country incoming) {
        if (incoming.getCode() != null)           existing.setCode(incoming.getCode());
        if (incoming.getName() != null)           existing.setName(incoming.getName());
        if (incoming.getOfficialName() != null)   existing.setOfficialName(incoming.getOfficialName());
        if (incoming.getIsoCode3() != null)       existing.setIsoCode3(incoming.getIsoCode3());
        if (incoming.getNumericCode() != null)    existing.setNumericCode(incoming.getNumericCode());
        if (incoming.getPhoneCode() != null)      existing.setPhoneCode(incoming.getPhoneCode());
        if (incoming.getCurrencyCode() != null)   existing.setCurrencyCode(incoming.getCurrencyCode());
        if (incoming.getCurrencySymbol() != null) existing.setCurrencySymbol(incoming.getCurrencySymbol());
        if (incoming.getContinent() != null)      existing.setContinent(incoming.getContinent());
    }

    @Override
    protected void onAfterCreate(Country saved) {
        outbox.publish(EventTypes.LOCATION_COUNTRY_CREATED, null, "country", saved.getId(),
                CountryEventPayload.from(saved));
    }

    @Override
    protected void onAfterUpdate(Country existing, Country updated) {
        outbox.publish(EventTypes.LOCATION_COUNTRY_UPDATED, null, "country", updated.getId(),
                CountryEventPayload.from(updated));
    }

    @Override
    protected void onAfterDelete(UUID id, Country snapshot) {
        outbox.publish(EventTypes.LOCATION_COUNTRY_DELETED, null, "country", id,
                CountryEventPayload.from(snapshot));
    }
}
