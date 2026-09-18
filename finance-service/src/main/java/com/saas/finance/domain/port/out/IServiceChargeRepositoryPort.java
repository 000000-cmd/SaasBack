package com.saas.finance.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.finance.domain.model.ChargeStatus;
import com.saas.finance.domain.model.ServiceCharge;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IServiceChargeRepositoryPort extends IGenericRepositoryPort<ServiceCharge, UUID> {

    List<ServiceCharge> findByEmployee(UUID employeeId);

    /**
     * Los cargos que ya existen para estas citas.
     *
     * <p>Es lo que hace idempotente al sincronizador: antes de crear pregunta
     * cuales ya tiene, y el indice unico sobre AppointmentId lo respalda por si
     * dos pasadas se cruzan.</p>
     */
    List<ServiceCharge> findByAppointments(Collection<UUID> appointmentIds);

    /** Aprobados y todavia sin liquidar: los que entran en la proxima liquidacion. */
    List<ServiceCharge> findSettlable(UUID employeeId);

    /** Pendientes de decision + aprobados sin liquidar: lo que sigue abierto. */
    List<ServiceCharge> findOpenByBusiness(UUID businessId);

    List<ServiceCharge> findHistory(UUID businessId, Collection<ChargeStatus> statuses,
                                    LocalDate from, LocalDate to, int page, int size);

    long countHistory(UUID businessId, Collection<ChargeStatus> statuses, LocalDate from, LocalDate to);

    /**
     * Los cargos DE ESE EMPLEADO sellados por unas liquidaciones concretas, en
     * orden de fecha. El empleado viaja aparte a proposito: es la garantia de que
     * un comprobante no puede listar el trabajo de otra persona.
     */
    List<ServiceCharge> findBySettlements(UUID employeeId, Collection<UUID> settlementIds);
}
