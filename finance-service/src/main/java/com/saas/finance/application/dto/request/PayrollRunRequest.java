package com.saas.finance.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Orden de dispersion de nomina.
 *
 * <p>No lleva montos: se paga el saldo a favor que cada empleado tiene en ese
 * momento. Dejar que el cliente mandara la cifra abriria la puerta a consignar
 * un numero distinto del que el empleado ve en su app.</p>
 *
 * <p>Cada linea SI tiene que traer la prueba del pago — el comprobante o la
 * marca de efectivo. El back lo rechaza si falta, no la pantalla: la misma
 * regla vale para la web, para el movil y para cualquier integracion futura.</p>
 */
public record PayrollRunRequest(
        @NotNull UUID businessId,
        UUID branchId,
        @NotEmpty(message = "Selecciona al menos un colaborador")
        @Valid List<Item> items,
        @Size(max = 255) String note
) {
    /**
     * @param bankAccountId  cuenta elegida del colaborador. Null si fue en efectivo.
     * @param payoutAccount  retrato en texto de esa cuenta, congelado para el comprobante.
     * @param paymentProofUrl foto del comprobante de la transferencia.
     * @param paidInCash     el dueño declara que pago en mano; entonces el
     *                       colaborador tendra que acusar recibo desde el movil.
     */
    public record Item(
            @NotNull UUID employeeId,
            UUID bankAccountId,
            @Size(max = 120) String payoutAccount,
            @Size(max = 500) String paymentProofUrl,
            /** SHA-256 del fichero. Lo devuelve la subida del adjunto. */
            @Size(max = 64) String paymentProofHash,
            Boolean paidInCash
    ) {
        /** Sin una de las dos no hay pago que registrar. */
        public boolean hasProof() {
            return Boolean.TRUE.equals(paidInCash)
                    || (paymentProofUrl != null && !paymentProofUrl.isBlank());
        }
    }
}
