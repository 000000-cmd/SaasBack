package com.saas.finance.domain.port.in;

import com.saas.finance.domain.model.ChargeStatus;
import com.saas.finance.domain.model.ServiceCharge;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IServiceChargeUseCase {

    /** Todos los servicios de un empleado: la pantalla filtra por estado y mes. */
    List<ServiceCharge> byEmployee(UUID employeeId);

    /**
     * Lo que sigue ABIERTO en todo el negocio: pendiente de decision o aprobado
     * pero aun sin liquidar. Alimenta el resumen y el asistente de liquidacion
     * masiva, que necesita las dos cosas a la vez (cuanto lleva aprobado cada
     * uno y cuanto le falta por revisar).
     */
    List<ServiceCharge> openByBusiness(UUID businessId);

    /**
     * Aprueba un servicio. {@code receiptUrl} es opcional y solo tiene sentido
     * en pagos electronicos: es el comprobante que se adjunta al confirmar.
     */
    ServiceCharge confirm(UUID id, String receiptUrl);

    /** Rechaza un servicio. No se borra: queda en el historial con su motivo. */
    ServiceCharge discard(UUID id, String reason);

    /** Deshace una aprobacion todavia no liquidada. Devuelve el cargo a pendiente. */
    ServiceCharge revert(UUID id, String reason);

    /**
     * Historial de lo ya resuelto, paginado EN SERVIDOR.
     *
     * @param status filtra a un unico estado; null trae confirmados y descartados.
     *        Va aqui y no en la pantalla porque filtrar una pagina ya cortada
     *        daria resultados que dependen de en que pagina estas.
     */
    List<ServiceCharge> history(UUID businessId, ChargeStatus status,
                                LocalDate from, LocalDate to, int page, int size);

    long countHistory(UUID businessId, ChargeStatus status, LocalDate from, LocalDate to);
}
