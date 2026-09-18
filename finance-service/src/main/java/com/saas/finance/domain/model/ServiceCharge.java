package com.saas.finance.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Un servicio prestado por un empleado: la unidad que se aprueba.
 *
 * <p>Antes se liquidaba un MONTO TOTAL, y con eso el dueno no podia ver que
 * estaba pagando, ni rechazar un servicio suelto, ni comprobar que la
 * transferencia de un cliente llego. Ahora la liquidacion es la suma de lo
 * aprobado, servicio por servicio.</p>
 *
 * <p>{@code appointmentId} va nulo y sin clave foranea: el modulo de citas
 * todavia no existe y la columna esta desde hoy para que el dia que llegue se
 * rellene sin migrar nada.</p>
 *
 * <p>Los tres importes se guardan CONGELADOS en vez de calcularse al vuelo: la
 * compensacion es versionada, y recalcular un servicio de marzo con la tarifa de
 * julio daria un numero distinto al que se acordo.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class ServiceCharge extends BaseDomain {

    private UUID businessId;
    private UUID branchId;
    private UUID employeeId;
    private UUID appointmentId;

    private String serviceName;
    private LocalDate serviceDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private String clientName;
    private UUID clientThirdPartyId;
    /** A donde se le manda su factura. Los dos opcionales: hay clientes de paso. */
    private String clientEmail;
    private String clientPhone;

    /** Lo que pago el cliente. */
    private BigDecimal grossAmount;
    /** % que se queda la empresa, congelado al momento del servicio. */
    private BigDecimal deductionRate;
    private BigDecimal deductionAmount;
    /** Lo que le queda al empleado: grossAmount - deductionAmount. */
    private BigDecimal netAmount;
    private String currency;

    private PaymentMethod paymentMethod;
    /** Comprobante del pago electronico. Obligatorio de hecho, no de esquema. */
    private String receiptUrl;
    /** Foto del resultado (como quedo el corte, las unas...). Siempre opcional. */
    private String resultPhotoUrl;

    private ChargeStatus status;
    private LocalDateTime confirmedAt;
    private LocalDateTime discardedAt;
    private String discardReason;

    /** Liquidacion que pago este cargo. Sin esto, "por que me pagaste esto" no tiene respuesta. */
    private UUID settlementId;

    /**
     * Si le falta el comprobante que su medio de pago exige.
     *
     * <p>Vive en el dominio y no en la pantalla porque la misma pregunta la
     * hacen el filtro de la tabla, la alerta de la fila y la ventana de
     * confirmacion. Tres copias de la regla se desincronizan; esta no.</p>
     */
    public boolean isMissingReceipt() {
        return paymentMethod != null
                && paymentMethod.isElectronic()
                && (receiptUrl == null || receiptUrl.isBlank());
    }
}
