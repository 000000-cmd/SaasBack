package com.saas.finance.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un movimiento del saldo del empleado: su extracto.
 *
 * <p>Una sola clase para las dos direcciones del dinero, porque para el empleado
 * son la misma lista. {@link MovementType} dice hacia donde va: los abonos
 * (liquidacion de servicios aprobados, sueldo base del periodo) suben el
 * devengado; la dispersion de nomina sube lo pagado.</p>
 *
 * <p>{@code balanceBefore} congela el saldo previo para poder auditar sin
 * recalcular historia, y {@code commissionAmount}/{@code baseSalaryAmount}
 * desglosan un pago de nomina: sin ese desglose el empleado ve un unico numero
 * y no sabe cuanto fue comision y cuanto sueldo.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class EmployeeSettlement extends BaseDomain {
    private UUID businessId;
    private UUID branchId;
    private UUID employeeId;
    /** Siempre positivo: el signo lo pone {@link #movementType}. */
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private String currency;
    private LocalDateTime settledAt;
    private String note;

    private MovementType movementType;
    /** Solo BASE_SALARY. Es la garantia de que un periodo se abona una vez. */
    private String periodKey;
    private UUID payrollRunId;
    private BigDecimal commissionAmount;
    private BigDecimal baseSalaryAmount;
    /** Retrato en texto de la cuenta usada, congelado para el comprobante. */
    private String payoutAccount;
    private UUID bankAccountId;

    /**
     * Prueba del pago. Un movimiento de nomina no existe sin una de las dos:
     * el comprobante de la transferencia, o la declaracion de que se pago en
     * mano. Antes esto era la palabra del dueño contra la del empleado.
     */
    private String paymentProofUrl;

    /**
     * SHA-256 del fichero del comprobante.
     *
     * <p>Sirve para saber cuando el MISMO soporte respalda varios pagos. No se
     * rechaza el repetido: un negocio que paga a cinco personas en una sola
     * transferencia tiene un unico soporte para los cinco, y eso es correcto.
     * Lo que hacia falta era que se notara.</p>
     */
    private String paymentProofHash;

    /**
     * El movimiento que este deshace, si es un contra-movimiento.
     *
     * <p>Unico en la tabla: un pago se deshace UNA vez. Sin esa clave, anular
     * dos veces la misma corrida devolveria el saldo dos veces, y la segunda es
     * dinero inventado.</p>
     */
    private UUID reversalOfId;
    private Boolean paidInCash;
    /** Acuse del empleado, SOLO en pagos en efectivo. */
    private LocalDateTime cashConfirmedAt;

    /** Efectivo entregado que el colaborador todavia no ha acusado. */
    public boolean isCashPendingConfirmation() {
        return Boolean.TRUE.equals(paidInCash) && cashConfirmedAt == null;
    }
}
