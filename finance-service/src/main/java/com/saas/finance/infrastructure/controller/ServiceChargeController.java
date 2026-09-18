package com.saas.finance.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.finance.application.dto.request.ConfirmChargeRequest;
import com.saas.finance.application.dto.request.DiscardChargeRequest;
import com.saas.finance.application.dto.response.ServiceChargeResponse;
import com.saas.finance.application.mapper.ServiceChargeMapper;
import com.saas.finance.domain.model.ChargeStatus;
import com.saas.finance.domain.port.in.IServiceChargeUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicios prestados: la unidad que el dueno aprueba o descarta antes de
 * liquidar. Es la pantalla de auditoria; mover dinero es {@code /settlements}.
 *
 * <h3>Aqui NO se marca un servicio como terminado, y es a proposito</h3>
 * <p>Hubo un {@code POST /{id}/complete} para que el empleado cerrara su
 * servicio desde el APK. Con la agenda dentro, eso creaba dos duenos del mismo
 * hecho: el cargo pasaba a pendiente mientras la cita seguia confirmada, asi
 * que el panel y la app contaban cosas distintas del mismo servicio.</p>
 *
 * <p>Ahora se termina la CITA ({@code POST /business/appointments/&#123;id&#125;/status}) y el
 * cargo la sigue en la siguiente pasada de {@code /sync}. Un solo sitio donde
 * consta que el servicio se presto.</p>
 */
@RestController
@RequestMapping("/service-charges")
@RequiredArgsConstructor
public class ServiceChargeController {

    private final IServiceChargeUseCase useCase;
    private final ServiceChargeMapper mapper;
    private final com.saas.finance.application.service.ClientInvoiceService invoices;
    private final com.saas.finance.application.service.AppointmentChargeSyncService sync;

    /**
     * Trae de la agenda los servicios que todavia no tienen cargo.
     *
     * <p>Va aparte del listado a proposito: una consulta no deberia escribir.
     * La pantalla lo llama al abrirse y despues pide la lista, asi que quien
     * mira siempre ve lo de hoy sin que un GET cree filas por su cuenta.</p>
     *
     * <p>Es idempotente: llamarlo dos veces no crea nada dos veces.</p>
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> sync(
            @RequestParam UUID businessId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate desde = from != null ? from : LocalDate.now().minusDays(7);
        LocalDate hasta = to != null ? to : LocalDate.now().plusDays(7);
        var r = sync.sync(businessId, desde, hasta);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "creados", r.creados(), "actualizados", r.actualizados(),
                "descartados", r.descartados(), "omitidos", r.omitidos())));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<ServiceChargeResponse>>> byEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.byEmployee(employeeId))));
    }

    @GetMapping("/open")
    public ResponseEntity<ApiResponse<List<ServiceChargeResponse>>> open(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.openByBusiness(businessId))));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<ServiceChargeResponse>> confirm(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ConfirmChargeRequest req) {
        String receipt = req == null ? null : req.receiptUrl();
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.confirm(id, receipt))));
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<ApiResponse<ServiceChargeResponse>> discard(
            @PathVariable UUID id, @Valid @RequestBody(required = false) DiscardChargeRequest req) {
        String reason = req == null ? null : req.reason();
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.discard(id, reason))));
    }

    /**
     * Deshace una aprobacion. Va aparte de {@code /discard} porque no es lo
     * mismo: descartar dice "esto no se cobra", reversar dice "me equivoque al
     * aprobarlo, hay que volver a mirarlo".
     */
    @PostMapping("/{id}/revert")
    public ResponseEntity<ApiResponse<ServiceChargeResponse>> revert(
            @PathVariable UUID id, @Valid @RequestBody DiscardChargeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponse(useCase.revert(id, req.reason())), "Aprobación reversada"));
    }

    /**
     * La factura del cliente, para verla desde el panel del negocio.
     *
     * <p>Es el MISMO documento que se le manda a el; tener dos generadores seria
     * tener dos facturas distintas para el mismo servicio.</p>
     */
    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> invoice(@PathVariable UUID id) {
        byte[] pdf = invoices.pdf(id);
        if (pdf == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                        org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + invoices.filename(id) + "\"")
                .body(pdf);
    }

    /** Le vuelve a mandar la factura al cliente, por donde haya dejado contacto. */
    @PostMapping("/{id}/invoice/send")
    public ResponseEntity<ApiResponse<Void>> sendInvoice(@PathVariable UUID id) {
        invoices.send(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Factura enviada al cliente"));
    }

    /**
     * Historial paginado de lo ya resuelto. El total viaja aparte porque la
     * pantalla necesita pintar el paginador antes de saber cuantas paginas hay.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> history(
            @RequestParam UUID businessId,
            @RequestParam(required = false) ChargeStatus status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "items", mapper.toResponseList(useCase.history(businessId, status, from, to, page, size)),
                "total", useCase.countHistory(businessId, status, from, to))));
    }
}
