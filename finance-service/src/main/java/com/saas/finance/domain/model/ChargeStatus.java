package com.saas.finance.domain.model;

/**
 * Estado de un servicio prestado dentro del ciclo de liquidacion.
 *
 * <p>DISCARDED no borra: un servicio que desaparece sin rastro es una discusion
 * asegurada con el empleado, asi que se queda en el historial con su motivo.</p>
 */
public enum ChargeStatus {
    /**
     * Agendado: la cita existe pero el servicio todavia no se ha prestado.
     *
     * <p>NO entra en la lista de aprobacion del dueno ni suma a ningun total:
     * no hay nada que aprobar hasta que ocurra. El empleado lo pasa a
     * {@link #PENDING} desde su app cuando lo termina, y ahi si aparece.</p>
     */
    SCHEDULED,
    /** Prestado, esperando que el dueno lo apruebe o lo descarte. */
    PENDING,
    /** Aprobado. Entra en la proxima liquidacion del empleado. */
    CONFIRMED,
    /** Rechazado por el dueno. No se paga, pero queda registrado. */
    DISCARDED
}
