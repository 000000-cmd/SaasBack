package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.infrastructure.persistence.repository.JpaAgendaLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Crea la fila del cerrojo EN SU PROPIA TRANSACCION, antes de que empiece la
 * pelea por el hueco.
 *
 * <h3>Por que existe esta clase</h3>
 * <p>El primer intento creaba la fila dentro de la transaccion de la reserva.
 * Con dos peticiones simultaneas al mismo hueco, las dos veian que la fila no
 * existia y las dos intentaban insertarla; en InnoDB, dos inserciones
 * concurrentes de la misma clave unica dentro de transacciones que ya sostienen
 * otros bloqueos se resuelven con un <b>deadlock</b>, no con un error de clave
 * duplicada.</p>
 *
 * <p>El resultado era correcto —solo una cita se creaba— pero la otra peticion
 * moria con un 500 diciendo "Deadlock found" en vez de "esa hora se acaba de
 * ocupar". Seguro, e inservible para quien esta reservando.</p>
 *
 * <h3>Como lo arregla</h3>
 * <p>{@code REQUIRES_NEW} abre una transaccion aparte que no hace nada mas que
 * insertar y confirmar. Es tan corta que no puede formar un ciclo de espera: si
 * la otra peticion se adelanto, esta choca contra el indice unico y recibe un
 * error de clave duplicada limpio, que se ignora porque significa exactamente
 * lo que hacia falta — la fila ya esta.</p>
 *
 * <p>Va en un componente aparte y no en un metodo del propio adaptador porque
 * {@code REQUIRES_NEW} solo abre transaccion nueva si la llamada cruza el proxy
 * de Spring. Llamandose a si mismo, la anotacion no hace nada y el problema
 * seguiria ahi, silencioso.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgendaLockInitializer {

    private final JpaAgendaLockRepository locks;

    /** Garantiza que la fila exista y este CONFIRMADA al volver. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists(UUID employeeId, LocalDate localDate) {
        if (locks.find(employeeId, localDate).isPresent()) return;
        // Cero filas = se adelanto otra peticion. No es un error: es el
        // resultado que se buscaba.
        int creadas = locks.insertIfAbsent(
                UUID.randomUUID().toString(), employeeId.toString(), localDate);
        if (creadas == 0) {
            log.debug("El cerrojo de {} el {} ya lo creo otra peticion", employeeId, localDate);
        }
    }
}
