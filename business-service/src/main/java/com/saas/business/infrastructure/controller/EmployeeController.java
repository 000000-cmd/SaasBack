package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.EmployeeProvisionRequest;
import com.saas.business.application.dto.request.EmployeeRequest;
import com.saas.business.application.dto.response.EmployeeDetailResponse;
import com.saas.business.application.dto.response.EmployeeProvisionResponse;
import com.saas.business.application.dto.response.EmployeeResponse;
import com.saas.business.application.mapper.EmployeeMapper;
import com.saas.business.application.service.EmployeeProvisioningService;
import com.saas.business.domain.model.Employee;
import com.saas.business.domain.port.in.IEmployeeUseCase;
import com.saas.business.infrastructure.client.SearchClient;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {
    private final IEmployeeUseCase useCase;
    private final EmployeeMapper mapper;
    private final ThirdPartyClient thirdPartyClient;
    private final SearchClient searchClient;
    private final EmployeeProvisioningService provisioningService;

    // Sin GET plano por sede: /detailed lo cubre (mismo listado + nombre de la
    // persona resuelto). Un solo endpoint de lectura por caso de uso.

    /**
     * Empleados de una sede con la persona resuelta (nombre + foto). Lectura de
     * VISUALIZACIÓN: viene del read model de Elasticsearch (search-service), no de
     * la BD transaccional de thirdparty. Si ES no responde, cae a thirdparty como
     * respaldo para no dejar el listado sin nombres.
     */
    @GetMapping("/detailed")
    public ResponseEntity<ApiResponse<List<EmployeeDetailResponse>>> detailedByBranch(@RequestParam UUID branchId) {
        List<Employee> emps = useCase.findByBranch(branchId);
        Set<UUID> ids = emps.stream().map(Employee::getThirdPartyId).collect(Collectors.toSet());
        final Map<String, PersonCard> resolved = resolveCards(ids);
        List<EmployeeDetailResponse> out = emps.stream()
                .map(e -> {
                    PersonCard c = resolved.get(e.getThirdPartyId().toString());
                    // Shell (sin nombre aún) → "" para que el front muestre
                    // "Pendiente de completar"; "—" solo si la persona no resuelve.
                    return new EmployeeDetailResponse(
                            e.getId(), e.getThirdPartyId(),
                            c == null ? "—" : (c.fullName() != null ? c.fullName() : ""),
                            c != null ? c.photoUrl() : null,
                            e.getBranchId(), e.getPositionId(), e.getSpecialtyId(), e.getEmployeeCode(),
                            e.getHireDate(), e.getTerminationDate(), e.getStatusId(), e.getEnabled());
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(out));
    }

    /** Tarjeta normalizada nombre+foto (independiente de la fuente). */
    private record PersonCard(String fullName, String photoUrl) {}

    /** Resuelve nombre+foto desde ES (read model); respaldo a thirdparty. */
    private Map<String, PersonCard> resolveCards(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        try {
            Map<String, SearchClient.PersonCard> es =
                    searchClient.personCards(ids.stream().map(UUID::toString).toList());
            if (es != null && !es.isEmpty()) {
                return es.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, en -> new PersonCard(en.getValue().fullName(), en.getValue().photoUrl())));
            }
        } catch (Exception ex) {
            log.debug("ES no resolvió personas, respaldo a thirdparty: {}", ex.getMessage());
        }
        try {
            return thirdPartyClient.personCards(ids).entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey, en -> new PersonCard(en.getValue().fullName(), en.getValue().photoUrl())));
        } catch (Exception ex) {
            log.debug("No se pudieron resolver personas: {}", ex.getMessage());
            return Map.of();
        }
    }
    @GetMapping("/third-party/{thirdPartyId}")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> byThirdParty(@PathVariable UUID thirdPartyId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByThirdParty(thirdPartyId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest req) {
        Employee created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    /**
     * Alta COMPLETA por el dueño (dashboard web): cuenta EMPLOYEE (auth) +
     * persona (thirdparty) + registro laboral. El empleado entra por el APK.
     */
    @PostMapping("/provision")
    public ResponseEntity<ApiResponse<EmployeeProvisionResponse>> provision(
            @Valid @RequestBody EmployeeProvisionRequest req) {
        return ResponseEntity.ok(ApiResponse.created(provisioningService.provision(req)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(@PathVariable UUID id, @Valid @RequestBody EmployeeRequest req) {
        Employee existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Empleado deshabilitado"));
    }
}
