package com.saas.finance.application.service;

import com.saas.common.events.EventTypes;
import com.saas.common.outbox.OutboxPublisher;
import com.saas.finance.application.dto.event.NotificationRequestPayload;
import com.saas.finance.domain.model.EmployeeBalance;
import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.finance.domain.model.PayrollRun;
import com.saas.finance.domain.model.ServiceCharge;
import com.saas.finance.infrastructure.client.BusinessInternalClient;
import com.saas.finance.infrastructure.client.ThirdPartyInternalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Los avisos de dinero al colaborador.
 *
 * <p>Publica al outbox y se acabo: no habla con Resend ni sabe que existe. El
 * evento viaja por Kafka hasta events-service, que resuelve la plantilla y
 * envia. Asi cambiar el texto del correo no toca este servicio, y este servicio
 * no se cae si el proveedor de correo esta caido.</p>
 *
 * <p>NADA de lo que pasa aqui puede tumbar el movimiento de dinero. Si falla
 * resolver un correo o generar el PDF, se registra y se sigue: el pago ya
 * ocurrio y deshacerlo por un aviso seria mucho peor que un aviso perdido.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceNotifier {

    private static final String AGGREGATE_TYPE = "employee_settlement";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OutboxPublisher outbox;
    private final ThirdPartyInternalClient people;
    private final BusinessInternalClient businesses;
    private final PayrollStatementPdf statements;
    private final PayoutServicesResolver payoutServices;

    /** Un pago de nomina con el saldo que le quedo al empleado. */
    public record Payout(EmployeeSettlement movement, EmployeeBalance after) {}

    /**
     * Regenera el extracto de UN movimiento, para descargarlo desde el
     * comprobante web.
     *
     * <p>Se vuelve a construir en vez de guardarlo: el PDF es una vista de datos
     * que ya estan congelados en el movimiento, asi que sale identico siempre y
     * no hay un fichero mas que respaldar, migrar y borrar.</p>
     *
     * @return el PDF cifrado con el documento del empleado, o null si no se pudo.
     */
    public byte[] statementOf(PayrollRun run, EmployeeSettlement movement, EmployeeBalance balance,
                              List<ServiceCharge> services) {
        ThirdPartyInternalClient.NotifyTarget target = targetOf(balance.getThirdPartyId());
        if (target == null) return null;
        return statements.build(run, movement, services, target.fullName(), target.documentNumber(),
                businessName(movement.getBusinessId()), balance.getBalance(), target.documentNumber());
    }

    /** Reenvia el aviso de un pago ya hecho, con su extracto adjunto. */
    public void resendPayrollNotice(PayrollRun run, EmployeeSettlement movement, EmployeeBalance balance) {
        payrollDispersed(run, List.of(new Payout(movement, balance)));
    }

    // =================================================================
    // Liquidacion: "te aprobamos N servicios y ya estan en tu saldo"
    // =================================================================

    public void settlementConfirmed(EmployeeSettlement settlement, int serviceCount, EmployeeBalance after) {
        try {
            UUID thirdPartyId = after.getThirdPartyId();
            ThirdPartyInternalClient.NotifyTarget target = targetOf(thirdPartyId);
            if (target == null || target.email() == null) {
                log.info("Liquidacion de {} sin correo verificado; no se notifica", settlement.getEmployeeId());
                return;
            }

            Map<String, String> data = new LinkedHashMap<>();
            data.put("EMPLEADO", target.fullName());
            data.put("NEGOCIO", businessName(settlement.getBusinessId()));
            data.put("MONTO", cop(settlement.getAmount()));
            data.put("SERVICIOS", String.valueOf(serviceCount));
            data.put("SALDO", cop(after.getBalance()));
            data.put("FECHA", settlement.getSettledAt().format(DAY));

            publish(settlement, new NotificationRequestPayload(
                    "SETTLEMENT_CONFIRMED", List.of(target.email()), thirdPartyId, data, null));

        } catch (Exception ex) {
            log.warn("No se pudo avisar de la liquidacion {}: {}", settlement.getId(), ex.getMessage());
        }
    }

    // =================================================================
    // Nomina: "te consignamos X" + el extracto en PDF
    // =================================================================

    public void payrollDispersed(PayrollRun run, List<Payout> payouts) {
        if (payouts.isEmpty()) return;
        try {
            Set<UUID> ids = payouts.stream()
                    .map(p -> p.after().getThirdPartyId())
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            if (ids.isEmpty()) return;

            // Una sola llamada para todo el equipo, no una por empleado.
            Map<String, ThirdPartyInternalClient.NotifyTarget> targets = people.notifyTargets(ids);
            String business = businessName(run.getBusinessId());

            for (Payout p : payouts) {
                UUID thirdPartyId = p.after().getThirdPartyId();
                ThirdPartyInternalClient.NotifyTarget target =
                        thirdPartyId == null ? null : targets.get(thirdPartyId.toString());
                if (target == null || target.email() == null) {
                    log.info("Nomina {}: {} sin correo verificado; no se notifica",
                            run.getCode(), p.movement().getEmployeeId());
                    continue;
                }

                Map<String, String> data = new LinkedHashMap<>();
                data.put("EMPLEADO", target.fullName());
                data.put("NEGOCIO", business);
                data.put("PERIODO", run.getPeriodLabel());
                data.put("MONTO", cop(p.movement().getAmount()));
                data.put("COMISION", cop(p.movement().getCommissionAmount()));
                data.put("SUELDO_BASE", cop(p.movement().getBaseSalaryAmount()));
                data.put("SALDO", cop(p.after().getBalance()));
                data.put("REFERENCIA", run.getCode());

                publish(p.movement(), new NotificationRequestPayload(
                        "PAYROLL_DISPERSED", List.of(target.email()), thirdPartyId, data,
                        statement(run, p, target, business)));
            }
        } catch (Exception ex) {
            log.warn("No se pudo avisar de la nomina {}: {}", run.getCode(), ex.getMessage());
        }
    }

    /**
     * El extracto adjunto, cifrado con el documento del empleado.
     *
     * <p>Va en base64 dentro del evento. Son unas decenas de KB y cabe de sobra
     * en un mensaje de Kafka; la alternativa (dejarlo en disco y mandar una URL)
     * obligaria a exponer ese fichero por HTTP, que es justo lo que se evita
     * poniendole contrasena.</p>
     */
    private NotificationRequestPayload.Attachment statement(
            PayrollRun run, Payout p, ThirdPartyInternalClient.NotifyTarget target, String business) {

        byte[] pdf = statements.build(run, p.movement(), payoutServices.of(p.movement()),
                target.fullName(), target.documentNumber(), business,
                p.after().getBalance(), target.documentNumber());
        if (pdf == null) return null;

        String name = "extracto-" + run.getCode().toLowerCase() + ".pdf";
        return new NotificationRequestPayload.Attachment(name, Base64.getEncoder().encodeToString(pdf));
    }

    // ---- utilidades ----

    private void publish(EmployeeSettlement movement, NotificationRequestPayload payload) {
        outbox.publish(EventTypes.NOTIFICATION_REQUESTED, movement.getBusinessId(),
                AGGREGATE_TYPE, movement.getId(), payload);
    }

    private ThirdPartyInternalClient.NotifyTarget targetOf(UUID thirdPartyId) {
        if (thirdPartyId == null) return null;
        return people.notifyTargets(Set.of(thirdPartyId)).get(thirdPartyId.toString());
    }

    private String businessName(UUID businessId) {
        try {
            return businesses.name(businessId).name();
        } catch (Exception ex) {
            log.debug("Sin nombre de negocio para {}: {}", businessId, ex.getMessage());
            return "Tu negocio";
        }
    }

    private static String cop(BigDecimal v) {
        BigDecimal n = v == null ? BigDecimal.ZERO : v;
        return "$ " + String.format("%,.0f", n).replace(',', '.');
    }
}
