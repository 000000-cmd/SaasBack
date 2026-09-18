package com.saas.finance.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.finance.domain.model.ChargeStatus;
import com.saas.finance.domain.model.ServiceCharge;
import com.saas.finance.domain.port.in.IServiceChargeUseCase;
import com.saas.finance.domain.port.out.IServiceChargeRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Aprobacion de servicios prestados, uno a uno.
 *
 * <p>Es la mitad "auditoria" del proceso: aqui el dueno decide QUE se paga. La
 * otra mitad (liquidar el total aprobado y abonarlo al saldo) vive en
 * {@link EmployeeSettlementService}, y separar las dos es lo que permite revisar
 * sin mover dinero.</p>
 *
 * <p>Un cargo ya liquidado no se puede tocar: cambiarlo desharia una cuenta que
 * el empleado ya vio en su saldo.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceChargeService implements IServiceChargeUseCase {

    private final IServiceChargeRepositoryPort repo;
    private final ClientInvoiceService invoices;

    @Override @Transactional(readOnly = true)
    public List<ServiceCharge> byEmployee(UUID employeeId) {
        return repo.findByEmployee(employeeId);
    }

    @Override @Transactional(readOnly = true)
    public List<ServiceCharge> openByBusiness(UUID businessId) {
        return repo.findOpenByBusiness(businessId);
    }

    @Override
    @Transactional
    public ServiceCharge confirm(UUID id, String receiptUrl) {
        ServiceCharge c = mutable(id);
        // Ya estaba aprobado: se devuelve tal cual. Repetirlo re-escribia la
        // fecha de aprobacion y VOLVIA A ENVIAR la factura al cliente, que es
        // lo que hace un doble clic de siempre.
        if (c.getStatus() == ChargeStatus.CONFIRMED) {
            log.debug("Servicio {} ya estaba aprobado; no se repite el aviso", id);
            return c;
        }
        // El comprobante puede llegar JUSTO al confirmar: es el caso del pago
        // electronico que no lo traia y el dueno lo adjunta en ese momento.
        if (receiptUrl != null && !receiptUrl.isBlank()) c.setReceiptUrl(receiptUrl);
        c.setStatus(ChargeStatus.CONFIRMED);
        c.setConfirmedAt(LocalDateTime.now());
        log.info("Servicio aprobado id={} empleado={} neto={}", id, c.getEmployeeId(), c.getNetAmount());
        ServiceCharge saved = repo.update(c);

        // La factura del cliente sale AQUI y no al crear el servicio: hasta que
        // el dueño no lo aprueba todavia puede descartarse, y anular una factura
        // ya enviada es peor que enviarla un rato mas tarde. No puede tumbar la
        // aprobacion: el servicio ya quedo aprobado pase lo que pase con el aviso.
        invoices.send(saved);
        return saved;
    }

    @Override
    @Transactional
    public ServiceCharge discard(UUID id, String reason) {
        // El motivo es obligatorio y no se rellena solo. "Descartado por el
        // dueño" no le explica nada al empleado al que le quitaron el servicio,
        // y es justo la conversacion que esto tiene que poder sostener.
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Escribe por qué lo rechazas: el empleado va a verlo");
        }
        ServiceCharge c = mutable(id);
        if (c.getStatus() == ChargeStatus.DISCARDED) return c;
        c.setStatus(ChargeStatus.DISCARDED);
        c.setDiscardedAt(LocalDateTime.now());
        c.setDiscardReason(reason);
        log.info("Servicio descartado id={} empleado={} motivo={}", id, c.getEmployeeId(), c.getDiscardReason());
        return repo.update(c);
    }

    /**
     * Deshace una aprobacion.
     *
     * <p>Solo antes de liquidar: {@code mutable} bloquea los ya liquidados,
     * porque ahi el abono ya esta en el saldo y deshacerlo es un
     * contra-movimiento del modulo de saldos, no un cambio de estado.</p>
     *
     * <p>Vuelve a PENDIENTE, no a descartado: aprobar por error no significa
     * que el servicio no se prestara. Queda pendiente de decidir otra vez, y el
     * motivo se guarda para que se vea que paso.</p>
     */
    @Override
    @Transactional
    public ServiceCharge revert(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Escribe por qué lo reversas");
        }
        ServiceCharge c = mutable(id);
        if (c.getStatus() != ChargeStatus.CONFIRMED) {
            throw new BusinessException("Solo se puede reversar un servicio aprobado");
        }
        c.setStatus(ChargeStatus.PENDING);
        // ConfirmedAt NO se borra: es un hecho, se aprobó ese día. Quien manda
        // sobre si está aprobado es el estado, y borrar la fecha destruiría la
        // única pista de que esto pasó por una aprobación.
        //
        // (Aunque se quisiera, no se podría: updateEntityFromDomain ignora los
        // nulos, así que ningún update de este adaptador puede vaciar un campo.)
        c.setDiscardReason("Aprobación reversada: " + reason);
        log.info("Aprobacion reversada id={} empleado={} motivo={}", id, c.getEmployeeId(), reason);
        return repo.update(c);
    }

    @Override @Transactional(readOnly = true)
    public List<ServiceCharge> history(UUID businessId, ChargeStatus status,
                                       LocalDate from, LocalDate to, int page, int size) {
        return repo.findHistory(businessId, scope(status), from, to, page, size);
    }

    @Override @Transactional(readOnly = true)
    public long countHistory(UUID businessId, ChargeStatus status, LocalDate from, LocalDate to) {
        return repo.countHistory(businessId, scope(status), from, to);
    }

    /** El historial es lo ya resuelto: aprobado o descartado, nunca pendiente. */
    private static final List<ChargeStatus> RESOLVED = List.of(ChargeStatus.CONFIRMED, ChargeStatus.DISCARDED);

    /**
     * Sin filtro, los dos estados resueltos. Con filtro, ese estado — pero nunca
     * PENDING: lo pendiente todavia se decide y su sitio es la liquidacion, no
     * el historial.
     */
    private static List<ChargeStatus> scope(ChargeStatus status) {
        return status == null || status == ChargeStatus.PENDING ? RESOLVED : List.of(status);
    }

    /**
     * Carga el cargo y comprueba que todavia se pueda cambiar de opinion sobre
     * el. Una vez liquidado el empleado ya lo vio en su saldo: reabrirlo seria
     * cambiar una cuenta cerrada.
     */
    private ServiceCharge mutable(UUID id) {
        ServiceCharge c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio prestado", "id", id));
        if (c.getSettlementId() != null) {
            throw new BusinessException("Este servicio ya fue liquidado y no se puede modificar");
        }
        return c;
    }
}
