package com.saas.business.domain.availability;

/**
 * Las reglas de reserva de un negocio. Espejo de {@code business_booking_policy}.
 *
 * <p>Es un record sin dependencias a proposito: el motor tiene que poder
 * probarse construyendo politicas a mano, sin base de datos ni Spring.</p>
 *
 * @param slotGranularityMinutes cada cuanto empieza un hueco. 15 = :00 :15 :30 :45.
 * @param bufferBeforeMinutes    preparacion antes de cada cita.
 * @param bufferAfterMinutes     limpieza despues de cada cita.
 * @param minLeadTimeMinutes     antelacion minima para reservar.
 * @param maxHorizonDays         hasta cuantos dias vista se puede reservar.
 */
public record BookingPolicy(
        int slotGranularityMinutes,
        int bufferBeforeMinutes,
        int bufferAfterMinutes,
        int minLeadTimeMinutes,
        int maxHorizonDays
) {
    public BookingPolicy {
        if (slotGranularityMinutes <= 0) {
            throw new IllegalArgumentException("La granularidad tiene que ser positiva");
        }
        if (bufferBeforeMinutes < 0 || bufferAfterMinutes < 0) {
            throw new IllegalArgumentException("Los buffers no pueden ser negativos");
        }
    }

    /** Lo que trae un negocio recien creado. Mismos valores que la migracion. */
    public static BookingPolicy defaults() {
        return new BookingPolicy(15, 0, 0, 30, 60);
    }
}
