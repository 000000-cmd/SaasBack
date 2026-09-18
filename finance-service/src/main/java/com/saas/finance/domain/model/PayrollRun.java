package com.saas.finance.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una dispersion de nomina: el lote con el que la empresa PAGA de verdad.
 *
 * <p>No hay id de transaccion bancaria porque el sistema no habla con el banco.
 * Lo que queda es la constancia interna ({@code code}) y, en cada movimiento, la
 * cuenta destino que el dueno anoto.</p>
 *
 * <p>{@code status} es COMPLETED si todos los empleados del lote se pagaron y
 * PARTIAL si alguno quedo fuera (su saldo cambio entre que se listo y se
 * ejecuto). No existe un "en proceso" persistido: la corrida se resuelve dentro
 * de una sola transaccion, y una fila colgada en PROCESSING seria mentira.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class PayrollRun extends BaseDomain {
    private UUID businessId;
    private UUID branchId;
    private String code;
    private String periodLabel;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer employeeCount;
    private BigDecimal totalAmount;
    private String currency;
    private String status;

    /** Cuando se anulo. Nulo = sigue en pie. */
    private LocalDateTime voidedAt;

    /** Por que se anulo. Obligatorio: una anulacion sin motivo no se audita. */
    private String voidReason;
    private LocalDateTime executedAt;
    private String note;
    /** Clave del envio. Dos envios del mismo formulario traen la misma. */
    private String idempotencyKey;

    public static final String COMPLETED = "COMPLETED";
    public static final String PARTIAL = "PARTIAL";

    /**
     * La corrida se deshizo.
     *
     * <p>NO se borra: la fila sigue diciendo que ese dia salio plata, porque
     * salio. Lo que la anulacion anade son los CONTRA-MOVIMIENTOS, y el saldo
     * vuelve por la suma de los dos — no por la desaparicion de uno.</p>
     */
    public static final String VOIDED = "ANULADA";
}
