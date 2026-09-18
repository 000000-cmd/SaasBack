package com.saas.business.application.service;

import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.business.domain.port.in.IBusinessBookingPolicyUseCase;
import com.saas.business.domain.port.out.IBusinessBookingPolicyRepositoryPort;
import com.saas.common.exception.BusinessException;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BusinessBookingPolicyService
        extends GenericCrudService<BusinessBookingPolicy, UUID>
        implements IBusinessBookingPolicyUseCase {

    private final IBusinessBookingPolicyRepositoryPort repo;

    public BusinessBookingPolicyService(IBusinessBookingPolicyRepositoryPort repo) {
        super(repo);
        this.repo = repo;
    }

    @Override protected String getResourceName() { return "Politica de reservas"; }

    @Override protected void applyChanges(BusinessBookingPolicy e, BusinessBookingPolicy i) {
        if (i.getRequiresManualConfirmation() != null) e.setRequiresManualConfirmation(i.getRequiresManualConfirmation());
        if (i.getConfirmationTimeoutMinutes() != null) e.setConfirmationTimeoutMinutes(i.getConfirmationTimeoutMinutes());
        if (i.getMinLeadTimeMinutes() != null) e.setMinLeadTimeMinutes(i.getMinLeadTimeMinutes());
        if (i.getMaxHorizonDays() != null) e.setMaxHorizonDays(i.getMaxHorizonDays());
        if (i.getSlotGranularityMinutes() != null) e.setSlotGranularityMinutes(i.getSlotGranularityMinutes());
        if (i.getBufferBeforeMinutes() != null) e.setBufferBeforeMinutes(i.getBufferBeforeMinutes());
        if (i.getBufferAfterMinutes() != null) e.setBufferAfterMinutes(i.getBufferAfterMinutes());
        if (i.getClientCancelWindowHours() != null) e.setClientCancelWindowHours(i.getClientCancelWindowHours());
        if (i.getRequirePhoneVerification() != null) e.setRequirePhoneVerification(i.getRequirePhoneVerification());
        // Cadena vacia SI es un valor: significa "este negocio no recuerda nada".
        // Por eso la guarda mira null y no isBlank.
        if (i.getReminderHoursBefore() != null) e.setReminderHoursBefore(i.getReminderHoursBefore());
        if (i.getWhatsappEnabled() != null) e.setWhatsappEnabled(i.getWhatsappEnabled());
        if (i.getWhatsappPhoneId() != null) e.setWhatsappPhoneId(i.getWhatsappPhoneId());
        // WhatsappMonthlyCount y WhatsappCountResetAt NO se tocan aqui: son
        // contadores del sistema, no configuracion. Dejarlos editables seria
        // dar un boton para poner a cero la factura de Meta.
    }

    /**
     * Las combinaciones que dejarian la agenda inservible.
     *
     * <p>Van aqui y no en anotaciones del DTO porque son relaciones entre
     * campos, no rangos sueltos.</p>
     */
    private void validar(BusinessBookingPolicy p) {
        if (p.getSlotGranularityMinutes() != null
                && (p.getSlotGranularityMinutes() < 5 || p.getSlotGranularityMinutes() > 120)) {
            throw new BusinessException("Los turnos deben empezar cada 5 a 120 minutos");
        }
        if (p.getMaxHorizonDays() != null && p.getMaxHorizonDays() < 1) {
            throw new BusinessException("Se tiene que poder reservar al menos con un día de vista");
        }
        if (Boolean.TRUE.equals(p.getRequiresManualConfirmation())
                && p.getConfirmationTimeoutMinutes() != null
                && p.getConfirmationTimeoutMinutes() < 5) {
            throw new BusinessException("Con confirmación manual, deja al menos 5 minutos para confirmar");
        }
    }

    @Override @Transactional(readOnly = true)
    public BusinessBookingPolicy forBusiness(UUID businessId) {
        return repo.findByBusinessId(businessId).orElseGet(() -> {
            BusinessBookingPolicy porDefecto = BusinessBookingPolicy.builder().build();
            porDefecto.setBusinessId(businessId);
            return porDefecto;
        });
    }

    /**
     * Guarda la configuracion. En los dos casos es un PARCHE, no un reemplazo:
     * lo que no venga se queda como estaba.
     *
     * <p>Al crear se arranca de los valores por defecto y se aplica encima lo
     * que llego. Insertar el objeto de la peticion tal cual escribiria nulos en
     * las columnas que la pantalla no mando —guardar solo "buffer de limpieza:
     * 10" dejaria la granularidad y el horizonte en nulo— y la fila no lo
     * admite.</p>
     */
    @Override @Transactional
    public BusinessBookingPolicy saveFor(UUID businessId, BusinessBookingPolicy incoming) {
        validar(incoming);
        return repo.findByBusinessId(businessId)
                .map(existente -> update(existente.getId(), incoming))
                .orElseGet(() -> {
                    BusinessBookingPolicy nueva = BusinessBookingPolicy.builder().build();
                    nueva.setBusinessId(businessId);
                    applyChanges(nueva, incoming);
                    return create(nueva);
                });
    }
}
