package com.saas.finance.domain.port.in;

import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.finance.domain.model.PayrollRun;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IPayrollUseCase {

    /**
     * Dispersa la nomina: a cada empleado se le consigna SU SALDO a favor y ese
     * saldo baja. Irreversible.
     *
     * <p>Cada linea tiene que traer su prueba de pago (comprobante o efectivo);
     * si a alguna le falta, NO se dispersa nada. Se rechaza la orden entera y no
     * solo esa linea: una corrida a medias deja al dueño sin saber a quien le
     * pago y a quien no.</p>
     */
    /**
     * Dispersa la nomina.
     *
     * @param idempotencyKey clave del envio. Si ya se dispersó con esta clave,
     *        se devuelve aquella corrida sin volver a pagar. Puede ser null.
     */
    PayrollRun disperse(UUID businessId, UUID branchId, List<PayoutOrder> orders,
                        String note, String idempotencyKey);

    /**
     * Una linea de la orden ya validada.
     *
     * @param payoutAccount retrato en texto de la cuenta, congelado para el comprobante.
     */
    /**
     * @param paymentProofHash SHA-256 del fichero del comprobante. Viaja para
     *        poder saber despues que el MISMO soporte respalda varios pagos —
     *        cosa legitima (una transferencia para cinco personas) pero que
     *        tiene que verse.
     */
    record PayoutOrder(UUID employeeId, UUID bankAccountId, String payoutAccount,
                       String paymentProofUrl, String paymentProofHash, boolean paidInCash) {}

    /**
     * El colaborador acusa recibo de un pago en efectivo. Solo el suyo: quien
     * llama tiene que demostrar que el movimiento es de su empleado.
     */
    EmployeeSettlement confirmCash(UUID movementId, UUID employeeId);

    List<PayrollRun> history(UUID businessId, LocalDate from, LocalDate to, int page, int size);

    long countHistory(UUID businessId, LocalDate from, LocalDate to);

    PayrollRun byId(UUID id);

    /**
     * Deshace una corrida de nomina.
     *
     * <p>NO borra nada: escribe un CONTRA-MOVIMIENTO por cada pago, devuelve el
     * saldo a cada empleado y marca la corrida como anulada. El libro sigue
     * diciendo que ese dia salio plata —porque salio— y ademas dice que se
     * deshizo.</p>
     *
     * <p>Se niega si algun pago en efectivo ya fue ACUSADO por su destinatario:
     * ahi el dinero esta fisicamente en su bolsillo, y devolverle el saldo
     * seria pagarle dos veces.</p>
     *
     * @param reason obligatorio. Una anulacion sin motivo no se audita.
     */
    PayrollRun voidRun(UUID id, String reason);
}
