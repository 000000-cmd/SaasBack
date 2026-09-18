package com.saas.finance.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Feign a business-service para leer la agenda.
 *
 * <p>Solo lectura y solo {@code /internal/**}. Finance no escribe nunca en la
 * agenda: pregunta que se presto y cobra por ello.</p>
 */
@FeignClient(name = "business-service", contextId = "business-agenda-finance", path = "/business")
public interface BusinessAgendaClient {

    @GetMapping("/internal/appointments/for-charges")
    List<AppointmentForCharge> forCharges(@RequestParam UUID businessId,
                                          @RequestParam String from,
                                          @RequestParam String to);

    /** Espejo del record que publica business. Los nombres tienen que coincidir. */
    record AppointmentForCharge(
            UUID appointmentId, UUID businessId, UUID branchId, UUID employeeId,
            String status, boolean backdated,
            LocalDate serviceDate, LocalTime startTime, LocalTime endTime,
            String serviceName, BigDecimal totalPrice, Integer totalMinutes,
            String clientName, String clientPhone, UUID clientThirdPartyId) {}
}
