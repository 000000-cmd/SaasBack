package com.saas.business.infrastructure.controller;

import com.saas.business.application.service.AvailabilityQueryService;
import com.saas.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Los huecos libres.
 *
 * <p>Es INFORMATIVO: que un hueco salga aqui no lo reserva. El bloqueo ocurre
 * al confirmar, y para entonces puede haberlo cogido otro. La pantalla tiene
 * que estar preparada para que la reserva devuelva {@code SLOT_NO_DISPONIBLE}
 * sobre un hueco que acaba de ensenar.</p>
 */
@RestController
@RequestMapping("/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityQueryService availability;

    /**
     * Un hueco, con quien lo atiende y en que dia LOCAL DEL NEGOCIO cae.
     *
     * <p>La fecha local la calcula el servidor. Agruparla en el front con la
     * zona del navegador repartiria las citas de la manana en el dia
     * equivocado en cuanto el cliente mire desde otro pais.</p>
     */
    public record SlotView(Instant startUtc, Instant endUtc, LocalDate localDate,
                           UUID employeeId, long minutes) {}

    public record AvailabilityView(String timeZone, List<SlotView> slots) {}

    @GetMapping
    public ResponseEntity<ApiResponse<AvailabilityView>> slots(
            @RequestParam UUID businessId,
            @RequestParam UUID branchId,
            @RequestParam List<UUID> offeringIds,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        AvailabilityQueryService.Result r = availability.slots(new AvailabilityQueryService.Query(
                businessId, branchId, offeringIds, employeeId, from, to));

        List<SlotView> vista = r.slots().stream()
                .map(s -> new SlotView(s.startUtc(), s.endUtc(),
                        s.startUtc().atZone(r.zone()).toLocalDate(), s.employeeId(),
                        Duration.between(s.startUtc(), s.endUtc()).toMinutes()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(
                new AvailabilityView(r.zone().getId(), vista)));
    }
}
