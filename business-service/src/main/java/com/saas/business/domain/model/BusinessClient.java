package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * El cliente DE UN NEGOCIO.
 *
 * <p>Sustituye a la antigua {@code Client}, que era un cliente global sin
 * vinculo con ningun negocio: existia, tenia controlador, y nadie la usaba.
 * Los datos del cliente vivian sueltos dentro de {@code service_charge}.</p>
 *
 * <p>La persona —quien es— vive en el esquema {@code personas}. Esto dice que
 * esa persona es cliente de ESTE negocio, y es lo unico que un negocio puede
 * consultar. Asi la regla de privacidad no depende de que alguien se acuerde
 * de filtrar: para llegar a una persona hay que pasar por aqui, y aqui ya esta
 * el {@code businessId}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class BusinessClient extends BaseDomain implements ITenantOwned {

    private UUID businessId;

    /**
     * Referencia a {@code personas.third_party}. Sin clave foranea: son dos
     * esquemas de dos servicios distintos.
     *
     * <p>NULL a proposito: es el cliente de mostrador, el que llega sin cita y
     * sin telefono. Tiene nombre para el recibo y nada mas. Darle un telefono
     * inventado "para que cuadre" ensuciaria la identidad por telefono de la
     * que depende todo lo demas.</p>
     */
    private UUID thirdPartyId;

    /** Como se le llama en la agenda. Para el de mostrador es lo unico que hay. */
    private String displayName;

    /**
     * Telefono en E.164 ({@code +573001234567}), SIEMPRE. Lo normaliza
     * {@code PhoneNumbers} en un solo punto: tres formas del mismo numero
     * serian tres clientes con el historial partido.
     */
    private String phoneE164;
    private LocalDateTime phoneVerifiedAt;

    /**
     * Consentimiento de WhatsApp. Se guarda CUANDO y QUE TEXTO acepto: sin las
     * dos cosas no hay forma de demostrar el opt-in si alguien reclama.
     */
    private LocalDateTime whatsappOptInAt;
    private String whatsappOptInText;
    /** La baja se respeta de verdad: con fecha aqui, no se le escribe mas. */
    private LocalDateTime whatsappOptOutAt;

    private String acquisitionSource;
    private String notes;

    /**
     * Contadores de comportamiento. Alimentan la politica de inasistencia sin
     * tener que recorrer la agenda entera en cada consulta.
     */
    private Integer noShowCount;
    private Integer visitCount;
    private LocalDateTime lastVisitAt;

    /** El de mostrador: sin persona detras y sin telefono. */
    public boolean isWalkIn() {
        return thirdPartyId == null && (phoneE164 == null || phoneE164.isBlank());
    }

    /** Puede recibir mensajes: dio permiso y no se ha dado de baja. */
    public boolean canReceiveWhatsapp() {
        return phoneE164 != null && whatsappOptInAt != null && whatsappOptOutAt == null;
    }

    /** El telefono esta confirmado como suyo. */
    public boolean isPhoneVerified() {
        return phoneVerifiedAt != null;
    }
}
