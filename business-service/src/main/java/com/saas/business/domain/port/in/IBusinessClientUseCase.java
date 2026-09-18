package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BusinessClient;
import com.saas.common.port.in.IGenericUseCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IBusinessClientUseCase extends IGenericUseCase<BusinessClient, UUID> {

    /** Los clientes de un negocio. La unica lectura que existe: siempre acotada. */
    List<BusinessClient> byBusiness(UUID businessId);

    /**
     * Busca por telefono dentro de un negocio. El telefono llega como lo
     * escribio quien sea; aqui se normaliza antes de buscar.
     */
    Optional<BusinessClient> findByPhone(UUID businessId, String rawPhone);

    /**
     * Encuentra al cliente por su telefono o lo crea. Es lo que usan la reserva
     * publica y el bot de WhatsApp: quien vuelve no genera una ficha nueva.
     */
    BusinessClient findOrCreateByPhone(UUID businessId, String rawPhone, String displayName);

    /**
     * Registra el consentimiento para recibir mensajes por WhatsApp.
     *
     * <p>Operacion propia y no un campo mas de la edicion de ficha: el
     * consentimiento lo mueve el flujo que lo provoca, y por eso
     * {@code applyChanges} no lo toca. Guarda la fecha Y el texto que la
     * persona vio, porque un opt-in sin constancia de que se aceptó no es un
     * opt-in.</p>
     *
     * <p>Idempotente: si ya lo habia dado, se respeta el primero. La fecha del
     * consentimiento es la del dia que lo dio, no la del ultimo formulario.</p>
     */
    BusinessClient recordWhatsappOptIn(UUID clientId, String acceptedText);

    /**
     * Deja constancia de que el telefono esta verificado.
     *
     * <p>Va aparte del {@code update} generico por lo mismo que el opt-in:
     * {@code applyChanges} EXCLUYE a proposito las marcas de verificacion, asi
     * que ponerlas con un setter y guardar no hace nada — responde bien y no
     * cambia la fila.</p>
     *
     * <p>Lo usa el bot de WhatsApp: si el mensaje llego DESDE ese numero, y
     * quien lo entrega es Meta, el numero esta probado. Pedirle ademas un
     * codigo por SMS seria pedirle que demuestre lo que acaba de demostrar.</p>
     */
    BusinessClient markPhoneVerified(UUID clientId);
}
