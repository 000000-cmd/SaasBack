package com.saas.business.domain.model;

import com.saas.common.exception.BusinessException;

import java.util.Set;

/**
 * Los estados por los que pasa una cita, y las transiciones que existen.
 *
 * <p>Estan aqui y no repartidos en condicionales por el codigo a proposito:
 * cualquier salto que no aparezca en esta tabla lanza error de dominio, asi que
 * no hay forma de que una pantalla nueva invente un camino que nadie previo —
 * cancelar algo ya completado, por ejemplo.</p>
 *
 * <h3>Reprogramar no esta aqui, y no es un olvido</h3>
 * <p>Reprogramar NO es una transicion: es cancelar la cita original con motivo
 * de reprogramacion y crear una nueva que apunta a ella. Si fuera un cambio de
 * fecha sobre la misma fila, la agenda perderia que alguien movio esa cita, y
 * esa es justo la pregunta que aparece cuando un cliente reclama.</p>
 */
public enum AppointmentStatus {

    /**
     * Reservada, esperando que el negocio la acepte.
     *
     * <p>Solo existe si el negocio activo la confirmacion manual. Con ella
     * apagada la cita nace {@link #CONFIRMADA} y este estado no aparece
     * nunca.</p>
     */
    PENDIENTE_CONFIRMACION,

    /** En pie. Es el estado normal de una cita futura. */
    CONFIRMADA,

    /** El servicio empezo. */
    EN_CURSO,

    /**
     * Terminada. Es la unica puerta al modulo de saldos: de aqui sale el cargo
     * con su comision, calculada del snapshot de la cita y no del catalogo
     * actual.
     */
    COMPLETADA,

    /** La cancelo el cliente. */
    CANCELADA_CLIENTE,

    /** La cancelo el negocio. */
    CANCELADA_NEGOCIO,

    /**
     * El cliente no llego.
     *
     * <p>No genera comision, pero SI queda en su historial y suma a su contador
     * de inasistencias. Borrarla seria perder justo el dato que sirve para
     * decidir si a alguien se le sigue reservando.</p>
     */
    NO_ASISTIO,

    /** Se agoto la ventana de confirmacion sin respuesta. Lo aplica un proceso. */
    EXPIRADA;

    /**
     * La cita OCUPA la agenda: cuenta para el solapamiento y resta
     * disponibilidad.
     *
     * <p>Las demas la liberan. Una cita cancelada tiene que devolver su hueco
     * en el momento, o el resto del dia queda bloqueado por algo que ya no
     * existe.</p>
     */
    public boolean occupiesAgenda() {
        return this == PENDIENTE_CONFIRMACION || this == CONFIRMADA || this == EN_CURSO;
    }

    /** Ya no se mueve de aqui. */
    public boolean isFinal() {
        return switch (this) {
            case COMPLETADA, CANCELADA_CLIENTE, CANCELADA_NEGOCIO, NO_ASISTIO, EXPIRADA -> true;
            default -> false;
        };
    }

    /** Se cerro sin prestarse el servicio. */
    public boolean isCancelled() {
        return this == CANCELADA_CLIENTE || this == CANCELADA_NEGOCIO || this == EXPIRADA;
    }

    /** A donde puede ir esta cita desde aqui. */
    public Set<AppointmentStatus> allowedNext() {
        return switch (this) {
            // CANCELADA_CLIENTE se admite tambien desde aqui, y esto se aparta
            // del documento original a proposito: quien reserva y cambia de
            // opinion antes de que el negocio conteste tiene que poder
            // cancelar. Sin esta transicion se quedaria esperando a que le
            // confirmen una cita que ya no quiere, o llamando por telefono.
            case PENDIENTE_CONFIRMACION -> Set.of(
                    CONFIRMADA, CANCELADA_CLIENTE, CANCELADA_NEGOCIO, EXPIRADA);

            case CONFIRMADA -> Set.of(
                    EN_CURSO, CANCELADA_CLIENTE, CANCELADA_NEGOCIO, NO_ASISTIO);

            // Ya empezo: no cabe una inasistencia, y el cliente no puede
            // cancelar algo que le estan haciendo.
            case EN_CURSO -> Set.of(COMPLETADA, CANCELADA_NEGOCIO);

            default -> Set.of();
        };
    }

    public boolean canGoTo(AppointmentStatus next) {
        return next != null && allowedNext().contains(next);
    }

    /**
     * Comprueba la transicion o falla con un mensaje que se entiende.
     *
     * <p>El mensaje dice el estado en que esta la cita y no solo "transicion
     * invalida": nueve de cada diez veces el problema es que otra persona ya la
     * movio, y saber a que estado la movio es la respuesta.</p>
     */
    public void ensureCanGoTo(AppointmentStatus next) {
        if (!canGoTo(next)) {
            throw new BusinessException(
                    "Esta cita está en " + legible() + " y desde ahí no se puede pasar a "
                            + (next == null ? "nada" : next.legible())
                            + ". Puede que alguien la haya movido antes.");
        }
    }

    /** Como se nombra en pantalla. */
    public String legible() {
        return switch (this) {
            case PENDIENTE_CONFIRMACION -> "pendiente de confirmar";
            case CONFIRMADA -> "confirmada";
            case EN_CURSO -> "en curso";
            case COMPLETADA -> "completada";
            case CANCELADA_CLIENTE -> "cancelada por el cliente";
            case CANCELADA_NEGOCIO -> "cancelada por el negocio";
            case NO_ASISTIO -> "no asistió";
            case EXPIRADA -> "expirada";
        };
    }

    /** Los estados que ocupan agenda. Lo usa la consulta de solapamiento. */
    public static Set<AppointmentStatus> occupying() {
        return Set.of(PENDIENTE_CONFIRMACION, CONFIRMADA, EN_CURSO);
    }
}
