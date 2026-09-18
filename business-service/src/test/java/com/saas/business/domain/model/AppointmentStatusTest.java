package com.saas.business.domain.model;

import com.saas.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static com.saas.business.domain.model.AppointmentStatus.CANCELADA_CLIENTE;
import static com.saas.business.domain.model.AppointmentStatus.CANCELADA_NEGOCIO;
import static com.saas.business.domain.model.AppointmentStatus.COMPLETADA;
import static com.saas.business.domain.model.AppointmentStatus.CONFIRMADA;
import static com.saas.business.domain.model.AppointmentStatus.EN_CURSO;
import static com.saas.business.domain.model.AppointmentStatus.EXPIRADA;
import static com.saas.business.domain.model.AppointmentStatus.NO_ASISTIO;
import static com.saas.business.domain.model.AppointmentStatus.PENDIENTE_CONFIRMACION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La maquina de estados de la cita: lo que se puede hacer y lo que no. */
class AppointmentStatusTest {

    @Test
    @DisplayName("Las transiciones validas son exactamente las declaradas")
    void transicionesValidas() {
        assertEquals(Set.of(CONFIRMADA, CANCELADA_CLIENTE, CANCELADA_NEGOCIO, EXPIRADA),
                PENDIENTE_CONFIRMACION.allowedNext());
        assertEquals(Set.of(EN_CURSO, CANCELADA_CLIENTE, CANCELADA_NEGOCIO, NO_ASISTIO),
                CONFIRMADA.allowedNext());
        assertEquals(Set.of(COMPLETADA, CANCELADA_NEGOCIO), EN_CURSO.allowedNext());
    }

    @Test
    @DisplayName("Desde un estado final no se mueve nada")
    void losFinalesSonFinales() {
        for (AppointmentStatus s : EnumSet.of(COMPLETADA, CANCELADA_CLIENTE,
                CANCELADA_NEGOCIO, NO_ASISTIO, EXPIRADA)) {
            assertTrue(s.isFinal(), s + " deberia ser final");
            assertTrue(s.allowedNext().isEmpty(), "desde " + s + " no se sale");
        }
    }

    @Test
    @DisplayName("Los saltos que no existen fallan, y el error dice donde esta la cita")
    void saltosInvalidos() {
        // Completar sin haber empezado.
        assertFalse(CONFIRMADA.canGoTo(COMPLETADA));
        // Resucitar una cancelada.
        assertFalse(CANCELADA_CLIENTE.canGoTo(CONFIRMADA));
        // Marcar inasistencia sobre algo que ya se hizo.
        assertFalse(COMPLETADA.canGoTo(NO_ASISTIO));
        // Volver atras.
        assertFalse(EN_CURSO.canGoTo(CONFIRMADA));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> COMPLETADA.ensureCanGoTo(NO_ASISTIO));
        assertTrue(ex.getMessage().contains("completada"),
                "el mensaje tiene que decir en que estado esta: " + ex.getMessage());
    }

    @Test
    @DisplayName("Solo tres estados ocupan la agenda; el resto liberan el hueco")
    void ocupacionDeAgenda() {
        assertEquals(Set.of(PENDIENTE_CONFIRMACION, CONFIRMADA, EN_CURSO),
                AppointmentStatus.occupying());

        for (AppointmentStatus s : AppointmentStatus.values()) {
            assertEquals(AppointmentStatus.occupying().contains(s), s.occupiesAgenda(),
                    s + " no coincide entre occupying() y occupiesAgenda()");
        }
        // Lo que de verdad importa: cancelar devuelve el hueco al momento.
        assertFalse(CANCELADA_CLIENTE.occupiesAgenda());
        assertFalse(CANCELADA_NEGOCIO.occupiesAgenda());
        assertFalse(EXPIRADA.occupiesAgenda());
        assertFalse(NO_ASISTIO.occupiesAgenda());
        assertFalse(COMPLETADA.occupiesAgenda());
    }

    @Test
    @DisplayName("El cliente puede cancelar antes de que le confirmen")
    void elClientePuedeCancelarLoPendiente() {
        // Se aparta del documento original a proposito: sin esto, quien reserva
        // y cambia de opinion antes de que el negocio conteste se queda
        // esperando una cita que ya no quiere.
        assertTrue(PENDIENTE_CONFIRMACION.canGoTo(CANCELADA_CLIENTE));
    }

    @Test
    @DisplayName("Una inasistencia no es una cancelacion: cuenta distinto")
    void inasistenciaNoEsCancelacion() {
        assertTrue(CANCELADA_CLIENTE.isCancelled());
        assertTrue(CANCELADA_NEGOCIO.isCancelled());
        assertTrue(EXPIRADA.isCancelled());
        // NO_ASISTIO libera el hueco igual, pero queda en el historial del
        // cliente y alimenta la politica de bloqueo. Meterla en el mismo saco
        // que una cancelacion borraria esa diferencia.
        assertFalse(NO_ASISTIO.isCancelled());
    }

    @Test
    @DisplayName("El camino feliz completo, paso a paso")
    void caminoFeliz() {
        AppointmentStatus s = PENDIENTE_CONFIRMACION;
        for (AppointmentStatus siguiente : new AppointmentStatus[]{CONFIRMADA, EN_CURSO, COMPLETADA}) {
            s.ensureCanGoTo(siguiente);
            s = siguiente;
        }
        assertEquals(COMPLETADA, s);
        assertTrue(s.isFinal());
    }

    @Test
    @DisplayName("Todos los estados tienen un nombre legible")
    void nombresLegibles() {
        for (AppointmentStatus s : AppointmentStatus.values()) {
            assertFalse(s.legible().isBlank(), s + " sin nombre legible");
        }
    }
}
