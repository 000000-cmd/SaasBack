package com.saas.finance.application.dto.response;

import com.saas.finance.domain.model.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un movimiento del extracto del empleado.
 *
 * <p>{@code movementType} es lo que permite a la web y al APK pintar un ingreso
 * distinto de un egreso; {@code commissionAmount}/{@code baseSalaryAmount}
 * desglosan un pago de nomina para que el empleado vea que ademas de la comision
 * se le consigno el sueldo base.</p>
 *
 * <p>{@code cashPendingConfirmation} lo calcula el dominio y no la pantalla: la
 * misma pregunta la hacen el aviso del movil, el pendiente del dueño y el
 * historial.</p>
 */
public record EmployeeSettlementResponse(
        UUID id, UUID businessId, UUID branchId, UUID employeeId,
        BigDecimal amount, BigDecimal balanceBefore, String currency,
        LocalDateTime settledAt, String note,
        MovementType movementType, String periodKey, UUID payrollRunId,
        BigDecimal commissionAmount, BigDecimal baseSalaryAmount,
        String payoutAccount, UUID bankAccountId,
        String paymentProofUrl, String paymentProofHash,
        Boolean paidInCash, LocalDateTime cashConfirmedAt,
        boolean cashPendingConfirmation,
        /** El movimiento que este deshace. No nulo solo en una anulacion. */
        UUID reversalOfId
) {}
