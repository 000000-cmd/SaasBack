package com.saas.business.domain.port.out;

import com.saas.business.domain.model.AppointmentNotice;

public interface IAppointmentNoticeRepositoryPort {

    /**
     * Deja la marca si nadie la habia dejado. Devuelve {@code true} solo la
     * primera vez, que es cuando toca avisar.
     *
     * <p>No lanza si ya existe: la insercion es un {@code INSERT IGNORE}. Una
     * excepcion de clave duplicada marcaria la transaccion para deshacerse, y
     * quien llama esta dentro de la transaccion que crea la cita — un aviso
     * repetido no puede costar la reserva.</p>
     */
    boolean markIfFirst(AppointmentNotice notice);
}
