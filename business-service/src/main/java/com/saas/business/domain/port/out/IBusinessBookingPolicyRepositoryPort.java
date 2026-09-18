package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.Optional;
import java.util.UUID;

public interface IBusinessBookingPolicyRepositoryPort
        extends IGenericRepositoryPort<BusinessBookingPolicy, UUID> {
    Optional<BusinessBookingPolicy> findByBusinessId(UUID businessId);

    /** El negocio dueno de un numero de WhatsApp. Lo usa el webhook. */
    Optional<BusinessBookingPolicy> findByWhatsappPhoneId(String phoneId);

    /** El negocio de una palabra de apreton de manos. Lo usa el alta del webhook. */
    Optional<BusinessBookingPolicy> findByWhatsappVerifyToken(String verifyToken);

    /**
     * Guarda (o borra) las credenciales de WhatsApp de un negocio.
     *
     * <p>Operacion propia y no {@code update()} porque {@code applyChanges} solo
     * asigna lo que llega distinto de null: con el, conectar no guardaria los
     * campos nuevos y DESCONECTAR no borraria nada — el token seguiria ahi
     * mientras la pantalla dice que ya no. Pasar {@code null} aqui SI borra.</p>
     */
    void saveWhatsappCredentials(UUID businessId, String phoneId, String wabaId,
                                 String tokenCifrado, String appSecretCifrado,
                                 String verifyToken, String displayPhone, String verifiedName,
                                 java.time.LocalDateTime verifiedAt, boolean enabled);

    /** Deja constancia de que el webhook quedo dado de alta contra Meta. */
    void saveWhatsappWebhook(UUID businessId, java.time.LocalDateTime cuando);
}
