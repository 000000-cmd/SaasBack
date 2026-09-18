package com.saas.finance.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.finance.application.dto.request.PayrollRunRequest;
import com.saas.finance.application.service.BaseSalaryAccrualService;
import com.saas.finance.application.service.PayrollDocumentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.saas.finance.application.dto.response.PayrollRunResponse;
import com.saas.finance.application.mapper.PayrollRunMapper;
import com.saas.finance.domain.port.in.IPayrollUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nomina: la dispersion real del dinero. Separada de {@code /settlements}
 * porque son dos decisiones distintas y mezclarlas hacia creer que liquidar
 * ya era pagar.
 */
@RestController
@RequestMapping("/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final IPayrollUseCase useCase;
    private final PayrollRunMapper mapper;
    private final BaseSalaryAccrualService baseSalary;
    private final PayrollDocumentService payrollDocs;
    private final com.saas.finance.application.mapper.EmployeeSettlementMapper settlementMapper;
    private final com.saas.finance.application.mapper.ServiceChargeMapper chargeMapper;

    /**
     * Dispersa la nomina.
     *
     * <p>Acepta una cabecera {@code Idempotency-Key}: si llegan dos peticiones
     * con la misma, la segunda devuelve la corrida de la primera en vez de
     * volver a pagar. La pantalla la genera una vez por formulario, no por
     * clic, que es lo que hace que el doble clic sea inofensivo.</p>
     */
    /**
     * Anula una dispersion.
     *
     * <p>No borra: escribe un contra-movimiento por pago y devuelve el saldo.
     * Solo el dueno o un administrador, y siempre con motivo.</p>
     */
    @PostMapping("/runs/{id}/void")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> voidRun(
            @PathVariable UUID id,
            @Valid @RequestBody VoidRunRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponse(useCase.voidRun(id, req.reason())),
                "Dispersión anulada y saldos devueltos"));
    }

    /** El motivo es obligatorio: una anulacion sin motivo no se audita. */
    public record VoidRunRequest(
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 300) String reason) {}

    @PostMapping("/runs")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> disperse(
            @Valid @RequestBody PayrollRunRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        List<IPayrollUseCase.PayoutOrder> orders = req.items().stream()
                .map(i -> new IPayrollUseCase.PayoutOrder(
                        i.employeeId(), i.bankAccountId(),
                        i.payoutAccount() == null ? null : i.payoutAccount().trim(),
                        i.paymentProofUrl(), i.paymentProofHash(),
                        Boolean.TRUE.equals(i.paidInCash())))
                .toList();
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(
                useCase.disperse(req.businessId(), req.branchId(), orders, req.note(),
                        idempotencyKey))));
    }

    /** Historial paginado. El total viaja aparte para poder pintar el paginador. */
    @GetMapping("/runs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> history(
            @RequestParam UUID businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "items", mapper.toResponseList(useCase.history(businessId, from, to, page, size)),
                "total", useCase.countHistory(businessId, from, to))));
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> byId(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.byId(id))));
    }

    /**
     * El comprobante completo: el pago y, DESGLOSADOS, los servicios que su
     * comisión cubre.
     *
     * <p>Los servicios van uno a uno y sin agrupar: "de dónde sale este número"
     * es la primera pregunta de quien recibe una factura, y un total suelto la
     * deja sin responder.</p>
     */
    @GetMapping("/movements/{id}/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@PathVariable UUID id) {
        PayrollDocumentService.StatementData d = payrollDocs.detail(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "movement", settlementMapper.toResponse(d.movement()),
                "run", mapper.toResponse(d.run()),
                "services", chargeMapper.toResponseList(d.services()))));
    }

    /**
     * El extracto en PDF de un movimiento, para descargarlo desde el comprobante.
     *
     * <p>Va cifrado con el documento del colaborador, igual que el que le llega
     * por correo: es el mismo papel, y un comprobante que se abre sin clave por
     * un lado y con clave por el otro no es el mismo comprobante.</p>
     */
    @GetMapping("/movements/{id}/statement")
    public ResponseEntity<byte[]> statement(@PathVariable UUID id) {
        byte[] pdf = payrollDocs.statement(id);
        if (pdf == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"extracto.pdf\"")
                .body(pdf);
    }

    /** Reenvía el comprobante por correo al colaborador. */
    @PostMapping("/movements/{id}/resend")
    public ResponseEntity<ApiResponse<Void>> resend(@PathVariable UUID id) {
        payrollDocs.resend(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Comprobante reenviado"));
    }

    /**
     * Lanza a mano el abono del sueldo base del periodo en curso.
     *
     * <p>La tarea programada ya lo hace cada madrugada; esto existe para no tener
     * que esperar a mañana cuando se acaba de configurar un sueldo, y para poder
     * comprobarlo. Es seguro repetirlo: la clave unica por periodo impide el
     * doble abono, asi que llamarlo diez veces abona una.</p>
     */
    @PostMapping("/base-salary/accrue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> accrueBaseSalary() {
        int credited = baseSalary.accrue(LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success(Map.of("credited", credited)));
    }
}
