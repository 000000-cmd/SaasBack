package com.saas.thirdparty.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.thirdparty.application.dto.request.BankAccountRequest;
import com.saas.thirdparty.application.dto.response.BankAccountResponse;
import com.saas.thirdparty.application.mapper.BankAccountMapper;
import com.saas.thirdparty.domain.model.BankAccount;
import com.saas.thirdparty.domain.port.in.IBankAccountUseCase;
import com.saas.thirdparty.infrastructure.persistence.repository.JpaBankAccountRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cuentas bancarias de una persona: a donde se le consigna la nomina.
 *
 * <p>Las lecturas devuelven el nombre del banco y la etiqueta ya compuestos, asi
 * que quien las pinta no necesita el catalogo de bancos aparte.</p>
 */
@RestController
@RequestMapping("/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final IBankAccountUseCase useCase;
    private final BankAccountMapper mapper;
    private final JpaBankAccountRepository views;

    /**
     * El catalogo de bancos, para el desplegable del alta.
     *
     * <p>Cuelga de aqui y no de {@code /system/list} porque {@code bank} nunca
     * se dio de alta como catalogo dinamico: esa ruta responde 404 y el
     * desplegable salia VACIO, con lo que no se podia elegir banco y por tanto
     * nadie podia registrar donde le pagan. Este servicio ya gobierna las
     * cuentas y ya lee esta tabla para componer la etiqueta.</p>
     */
    @GetMapping("/banks")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> banks() {
        List<Map<String, String>> catalogo = views.findBanks().stream()
                .map(b -> Map.of("id", b.getId(), "code", b.getCode(), "name", b.getName()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(catalogo));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BankAccountResponse>>> byThirdParty(@RequestParam UUID thirdPartyId) {
        return ResponseEntity.ok(ApiResponse.success(
                mapper.fromViews(views.findViewByThirdParties(List.of(thirdPartyId.toString())))));
    }

    /**
     * Cuentas de varias personas de una vez, agrupadas por tercero.
     *
     * <p>Es POST y no GET porque la lista de ids puede ser larga y no cabe
     * comoda en la URL. La usa el asistente de nomina: una llamada por corrida
     * en vez de una por empleado.</p>
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Map<String, List<BankAccountResponse>>>> batch(
            @RequestBody Set<UUID> thirdPartyIds) {
        if (thirdPartyIds == null || thirdPartyIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(Map.of()));
        }
        Map<String, List<BankAccountResponse>> grouped = mapper
                .fromViews(views.findViewByThirdParties(
                        thirdPartyIds.stream().map(UUID::toString).toList()))
                .stream()
                .collect(Collectors.groupingBy(a -> a.thirdPartyId().toString()));
        return ResponseEntity.ok(ApiResponse.success(grouped));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankAccountResponse>> create(@Valid @RequestBody BankAccountRequest req) {
        BankAccount created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(one(created.getId())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccountResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody BankAccountRequest req) {
        BankAccount existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        useCase.update(id, existing);
        return ResponseEntity.ok(ApiResponse.success(one(id)));
    }

    /** Marcar la principal es su propia acción: es un gesto, no una edición. */
    @PutMapping("/{id}/primary")
    public ResponseEntity<ApiResponse<BankAccountResponse>> makePrimary(@PathVariable UUID id) {
        useCase.makePrimary(id);
        return ResponseEntity.ok(ApiResponse.success(one(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Cuenta eliminada"));
    }

    /** Relee por la proyección para devolver siempre la misma forma que el GET. */
    private BankAccountResponse one(UUID id) {
        BankAccount a = useCase.getById(id);
        return mapper.fromViews(views.findViewByThirdParties(List.of(a.getThirdPartyId().toString())))
                .stream().filter(r -> r.id().equals(id)).findFirst().orElse(null);
    }
}
