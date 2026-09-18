package com.saas.business.application.service;

import com.saas.business.domain.model.BusinessClient;
import com.saas.business.domain.port.in.IBusinessClientUseCase;
import com.saas.business.domain.port.out.IBusinessClientRepositoryPort;
import com.saas.common.exception.BusinessException;
import com.saas.common.service.GenericCrudService;
import com.saas.common.util.PhoneNumbers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Los clientes de un negocio.
 *
 * <p>Toda entrada de telefono pasa por {@link PhoneNumbers#toE164}: es el
 * unico punto donde se normaliza. Si se hiciera en cada pantalla, "300 123
 * 4567" desde la web y "3001234567" desde WhatsApp serian dos clientes con el
 * historial partido, y nadie lo notaria hasta que alguien reclame.</p>
 */
@Service
public class BusinessClientService
        extends GenericCrudService<BusinessClient, UUID>
        implements IBusinessClientUseCase {

    private final IBusinessClientRepositoryPort repo;

    public BusinessClientService(IBusinessClientRepositoryPort repo) {
        super(repo);
        this.repo = repo;
    }

    @Override
    protected String getResourceName() { return "Cliente"; }

    @Override
    protected void onBeforeCreate(BusinessClient entity) {
        if (entity.getBusinessId() == null) {
            throw new BusinessException("Un cliente siempre pertenece a un negocio");
        }
        if (entity.getDisplayName() == null || entity.getDisplayName().isBlank()) {
            throw new BusinessException("El cliente necesita un nombre para aparecer en la agenda");
        }
        entity.setPhoneE164(normalizarOFallar(entity.getPhoneE164()));

        // Un telefono es UN cliente dentro del negocio. La base tambien lo
        // impone con un indice unico; comprobarlo aqui permite dar un mensaje
        // que se entienda en vez de un choque de clave.
        if (entity.getPhoneE164() != null
                && repo.findByBusinessAndPhone(entity.getBusinessId(), entity.getPhoneE164()).isPresent()) {
            throw new BusinessException("Ya tienes un cliente con ese teléfono");
        }
        if (entity.getVisitCount() == null) entity.setVisitCount(0);
        if (entity.getNoShowCount() == null) entity.setNoShowCount(0);
    }

    @Override
    protected void onBeforeUpdate(BusinessClient existing, BusinessClient incoming) {
        if (incoming.getPhoneE164() != null) {
            String nuevo = normalizarOFallar(incoming.getPhoneE164());
            incoming.setPhoneE164(nuevo);
            boolean cambia = nuevo != null && !nuevo.equals(existing.getPhoneE164());
            if (cambia && repo.findByBusinessAndPhone(existing.getBusinessId(), nuevo)
                              .filter(otro -> !otro.getId().equals(existing.getId()))
                              .isPresent()) {
                throw new BusinessException("Ya tienes otro cliente con ese teléfono");
            }
            // Cambiar el telefono INVALIDA la verificacion: el numero nuevo no
            // lo ha confirmado nadie todavia.
            if (cambia) existing.setPhoneVerifiedAt(null);
        }
    }

    @Override
    protected void applyChanges(BusinessClient existing, BusinessClient incoming) {
        if (incoming.getDisplayName() != null)       existing.setDisplayName(incoming.getDisplayName());
        if (incoming.getPhoneE164() != null)         existing.setPhoneE164(incoming.getPhoneE164());
        if (incoming.getThirdPartyId() != null)      existing.setThirdPartyId(incoming.getThirdPartyId());
        if (incoming.getAcquisitionSource() != null) existing.setAcquisitionSource(incoming.getAcquisitionSource());
        if (incoming.getNotes() != null)             existing.setNotes(incoming.getNotes());
        // Los contadores y las marcas de consentimiento NO se tocan desde una
        // edicion de ficha: los mueve el flujo que los provoca (una cita
        // completada, un opt-in aceptado). Dejarlos editables aqui permitiria
        // borrar a mano una inasistencia o un consentimiento.
    }

    @Override
    @Transactional
    public BusinessClient recordWhatsappOptIn(UUID clientId, String acceptedText) {
        BusinessClient c = getById(clientId);
        if (c.getWhatsappOptInAt() != null) return c;   // ya lo dio; vale el primero
        c.setWhatsappOptInAt(java.time.LocalDateTime.now());
        c.setWhatsappOptInText(acceptedText);
        c.setWhatsappOptOutAt(null);
        return repo.update(c);
    }

    @Override
    @Transactional
    public BusinessClient markPhoneVerified(UUID clientId) {
        BusinessClient c = getById(clientId);
        if (c.getPhoneVerifiedAt() != null) return c;   // ya estaba; vale la primera
        c.setPhoneVerifiedAt(java.time.LocalDateTime.now());
        return repo.update(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessClient> byBusiness(UUID businessId) {
        return repo.findByBusinessId(businessId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BusinessClient> findByPhone(UUID businessId, String rawPhone) {
        String e164 = PhoneNumbers.toE164(rawPhone);
        return e164 == null ? Optional.empty() : repo.findByBusinessAndPhone(businessId, e164);
    }

    /**
     * Lo que usan la reserva publica y el bot: quien vuelve a reservar con el
     * mismo telefono cae en su ficha de siempre, con su historial.
     */
    @Override
    @Transactional
    public BusinessClient findOrCreateByPhone(UUID businessId, String rawPhone, String displayName) {
        String e164 = normalizarOFallar(rawPhone);
        if (e164 == null) {
            throw new BusinessException("Hace falta un teléfono para poder avisarte de tu cita");
        }
        return repo.findByBusinessAndPhone(businessId, e164)
                .map(existente -> {
                    // El nombre se actualiza si venia vacio: la primera reserva
                    // pudo haber sido de mostrador, sin nombre real.
                    if ((existente.getDisplayName() == null || existente.getDisplayName().isBlank())
                            && displayName != null && !displayName.isBlank()) {
                        existente.setDisplayName(displayName.trim());
                        return repo.update(existente);
                    }
                    return existente;
                })
                .orElseGet(() -> repo.save(BusinessClient.builder()
                        .businessId(businessId)
                        .displayName(displayName == null || displayName.isBlank()
                                ? PhoneNumbers.toLocalDisplay(e164) : displayName.trim())
                        .phoneE164(e164)
                        .visitCount(0)
                        .noShowCount(0)
                        // enabled/visible los pone BaseDomain en TRUE: el
                        // @Builder de Lombok no alcanza los campos del padre.
                        .build()));
    }

    /**
     * Normaliza, o falla con un mensaje que se entiende.
     *
     * <p>Un telefono en blanco es valido —el cliente de mostrador no tiene—,
     * pero uno escrito mal no se guarda "como venga": eso es lo que crea los
     * duplicados que despues nadie sabe unir.</p>
     */
    private static String normalizarOFallar(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String e164 = PhoneNumbers.toE164(raw);
        if (e164 == null) {
            throw new BusinessException("Ese teléfono no parece válido: son 10 dígitos");
        }
        return e164;
    }
}
