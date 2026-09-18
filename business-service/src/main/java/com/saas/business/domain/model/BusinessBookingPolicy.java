package com.saas.business.domain.model;

import com.saas.business.domain.availability.BookingPolicy;
import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Las reglas de reserva de un negocio. Una fila por negocio.
 *
 * <p>Estan en base de datos y no como constantes porque una barberia con dos
 * sillas y un centro de estetica con cabinas no reservan igual, y quien lo sabe
 * es el dueno, no el programador.</p>
 *
 * <p>{@code businessId} lo hereda de {@link BaseDomain}.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class BusinessBookingPolicy extends BaseDomain {

    @Builder.Default private Boolean requiresManualConfirmation = Boolean.FALSE;
    @Builder.Default private Integer confirmationTimeoutMinutes = 120;
    @Builder.Default private Integer minLeadTimeMinutes = 30;
    @Builder.Default private Integer maxHorizonDays = 60;
    @Builder.Default private Integer slotGranularityMinutes = 15;
    @Builder.Default private Integer bufferBeforeMinutes = 0;
    @Builder.Default private Integer bufferAfterMinutes = 0;
    @Builder.Default private Integer clientCancelWindowHours = 4;
    /** Exigir telefono verificado para reservar desde la web publica. */
    @Builder.Default private Boolean requirePhoneVerification = Boolean.TRUE;

    /**
     * Cuantas horas antes se recuerda la cita, separadas por coma ("24,2").
     * Vacio = ese negocio no manda recordatorios.
     *
     * <p>Es una cadena y no una columna por recordatorio porque cuantos manda
     * cada negocio es configurable: con columnas, anadir "tambien 2 horas
     * antes" seria una migracion.</p>
     */
    @Builder.Default private String reminderHoursBefore = "24";

    @Builder.Default private Boolean whatsappEnabled = Boolean.FALSE;

    /**
     * Id del numero de WhatsApp Business de este negocio (lo da Meta).
     *
     * <p>Es como se resuelve a quien le escribieron: el webhook trae el numero
     * de destino, no el negocio. Unico en toda la plataforma — dos negocios con
     * el mismo numero serian dos agendas recibiendo la misma conversacion.</p>
     */
    private String whatsappPhoneId;
    /** Token de la Cloud API, CIFRADO. Nunca sale hacia la pantalla. */
    /**
     * La CUENTA de WhatsApp Business (WABA) a la que pertenece ese numero.
     * Hace falta para dar de alta el webhook por API en vez de a mano.
     */
    private String whatsappWabaId;
    private String whatsappAccessToken;
    /** Secreto de la app de Meta del negocio, CIFRADO. Verifica la firma del webhook. */
    private String whatsappAppSecret;
    /** Palabra del apreton de manos. En claro: hay que ensenarsela al dueno. */
    private String whatsappVerifyToken;
    /** Cuando se dio de alta el webhook contra Meta. Nulo = hay que hacerlo a mano. */
    private LocalDateTime whatsappWebhookAt;
    private String whatsappDisplayPhone;
    private String whatsappVerifiedName;
    private LocalDateTime whatsappVerifiedAt;
    @Builder.Default private Integer whatsappMonthlyCount = 0;
    private LocalDateTime whatsappCountResetAt;

    /**
     * Las horas de antelacion ya leidas, ordenadas de mas lejana a mas cercana.
     * Lo que no sea un numero positivo se ignora en silencio: una configuracion
     * mal escrita no puede dejar al negocio sin los recordatorios que si estan
     * bien.
     */
    public java.util.List<Integer> reminderHours() {
        if (reminderHoursBefore == null || reminderHoursBefore.isBlank()) return java.util.List.of();
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (String parte : reminderHoursBefore.split(",")) {
            try {
                int h = Integer.parseInt(parte.trim());
                if (h > 0 && !out.contains(h)) out.add(h);
            } catch (NumberFormatException ignored) {
                // configuracion mal escrita: se salta esa, no todas
            }
        }
        out.sort(java.util.Comparator.reverseOrder());
        return out;
    }

    /** Lo que el motor de disponibilidad necesita, y nada mas. */
    public BookingPolicy toEnginePolicy() {
        return new BookingPolicy(
                slotGranularityMinutes, bufferBeforeMinutes, bufferAfterMinutes,
                minLeadTimeMinutes, maxHorizonDays);
    }
}
