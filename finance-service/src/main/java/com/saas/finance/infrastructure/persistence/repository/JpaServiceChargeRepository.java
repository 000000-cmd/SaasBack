package com.saas.finance.infrastructure.persistence.repository;

import com.saas.finance.domain.model.ChargeStatus;
import com.saas.finance.infrastructure.persistence.entity.ServiceChargeEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaServiceChargeRepository extends JpaRepository<ServiceChargeEntity, UUID> {

    List<ServiceChargeEntity> findByEmployeeIdOrderByServiceDateDescStartTimeDesc(UUID employeeId);

    List<ServiceChargeEntity> findByAppointmentIdIn(Collection<UUID> appointmentIds);

    /** Los que entran en la proxima liquidacion: aprobados y todavia sin liquidar. */
    List<ServiceChargeEntity> findByEmployeeIdAndStatusAndSettlementIdIsNull(UUID employeeId, ChargeStatus status);

    /**
     * Lo ABIERTO del negocio: pendiente de decision o aprobado pero sin liquidar.
     * Es lo que el dueño todavia tiene entre manos, y lo unico que necesitan el
     * resumen y el asistente de liquidacion masiva.
     */
    List<ServiceChargeEntity> findByBusinessIdAndStatusInAndSettlementIdIsNull(
            UUID businessId, Collection<ChargeStatus> statuses);

    /** Historial: ya resueltos (aprobados o descartados) en un rango de fechas. */
    List<ServiceChargeEntity> findByBusinessIdAndStatusInAndServiceDateBetweenOrderByServiceDateDesc(
            UUID businessId, Collection<ChargeStatus> statuses, LocalDate from, LocalDate to, Pageable pageable);

    long countByBusinessIdAndStatusInAndServiceDateBetween(
            UUID businessId, Collection<ChargeStatus> statuses, LocalDate from, LocalDate to);

    /**
     * El detalle de un comprobante: que servicios pago una liquidacion.
     *
     * <p>Filtra TAMBIEN por empleado aunque las liquidaciones ya sean suyas. Es
     * redundante a proposito: en una factura, un servicio de otra persona no es
     * un fallo de pintado sino dinero atribuido a quien no lo trabajo, y el sitio
     * barato de impedirlo es la consulta.</p>
     */
    List<ServiceChargeEntity> findByEmployeeIdAndSettlementIdInOrderByServiceDateAscStartTimeAsc(
            UUID employeeId, Collection<UUID> settlementIds);
}
