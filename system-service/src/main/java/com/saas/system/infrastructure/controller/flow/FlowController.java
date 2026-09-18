package com.saas.system.infrastructure.controller.flow;

import com.saas.common.dto.ApiResponse;
import com.saas.common.security.IUserPrincipal;
import com.saas.system.application.dto.request.flow.FlowControlRequest;
import com.saas.system.application.dto.request.flow.FlowFieldRequest;
import com.saas.system.application.dto.request.flow.FlowRequest;
import com.saas.system.application.dto.request.flow.FlowSectionRequest;
import com.saas.system.application.dto.response.flow.FlowAdminView;
import com.saas.system.application.dto.response.flow.ResolvedFlowView;
import com.saas.system.application.service.flow.FlowAdminService;
import com.saas.system.application.service.flow.FlowResolverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Flujos: la pestana de configuracion y el flujo resuelto que consume el front.
 *
 * <p>Dos vistas del mismo dato y a proposito. La de configuracion ensena TODO,
 * incluido lo desactivado, porque quien configura tiene que poder volver a
 * encenderlo. La resuelta ensena solo lo que toca pintar en ese canal a esa
 * persona.</p>
 */
@RestController
@RequestMapping("/flows")
@RequiredArgsConstructor
public class FlowController {

    private final FlowAdminService admin;
    private final FlowResolverService resolver;

    // ------------------------------------------------------------- consumo

    /**
     * El flujo listo para pintar.
     *
     * <p>Lo llaman la web, el panel, el APK y el bot. No exige rol de
     * administrador: es la configuracion de una pantalla que ya se esta
     * mostrando, y los botones vienen filtrados por los permisos de quien
     * pregunta.</p>
     */
    @GetMapping("/{code}/resolved")
    public ResponseEntity<ApiResponse<ResolvedFlowView>> resolved(
            @PathVariable String code,
            @RequestParam(defaultValue = "WEB") String channel) {
        return ResponseEntity.ok(ApiResponse.success(resolver.resolve(code, channel, roles())));
    }

    /**
     * El flujo como lo veria OTRO: otro canal, otros roles.
     *
     * <p>Es la vista previa de la pantalla de configuracion. Llama al MISMO
     * resolvedor que usan la web, el panel, el APK y el bot — no a una copia —,
     * asi que lo que se ve aqui es exactamente lo que va a salir. Una vista
     * previa que reimplementara el filtrado seria una segunda verdad, y la
     * primera vez que las dos discreparan, la que se cree es la falsa.</p>
     *
     * <p>Solo administrador: pedir "damelo como si fuera OWNER" es justo lo que
     * no puede poder hacer cualquiera.</p>
     *
     * @param roles codigos separados por coma. Vacio = nadie, que es lo que ve
     *        quien entra sin sesion (la reserva publica).
     */
    @GetMapping("/{code}/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResolvedFlowView>> preview(
            @PathVariable String code,
            @RequestParam(defaultValue = "WEB") String channel,
            @RequestParam(required = false) String roles) {

        Set<String> comoQuien = (roles == null || roles.isBlank())
                ? Set.of()
                : Arrays.stream(roles.split(",")).map(String::trim)
                        .filter(r -> !r.isEmpty()).collect(Collectors.toSet());

        return ResponseEntity.ok(ApiResponse.success(
                resolver.resolve(code, channel, comoQuien)));
    }

    // ------------------------------------------------- configuracion: flujo

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FlowAdminView.Summary>>> list() {
        return ResponseEntity.ok(ApiResponse.success(admin.list()));
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> tree(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(admin.tree(code)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> create(@Valid @RequestBody FlowRequest req) {
        return ResponseEntity.ok(ApiResponse.created(admin.createFlow(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> update(
            @PathVariable UUID id, @Valid @RequestBody FlowRequest req) {
        return ResponseEntity.ok(ApiResponse.success(admin.updateFlow(id, req), "Flujo guardado"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        admin.deleteFlow(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Flujo eliminado"));
    }

    // ---------------------------------------------- configuracion: seccion

    @PostMapping("/{flowId}/sections")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> createSection(
            @PathVariable UUID flowId, @Valid @RequestBody FlowSectionRequest req) {
        return ResponseEntity.ok(ApiResponse.created(admin.createSection(flowId, req)));
    }

    @PutMapping("/sections/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> updateSection(
            @PathVariable UUID id, @Valid @RequestBody FlowSectionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(admin.updateSection(id, req), "Sección guardada"));
    }

    @DeleteMapping("/sections/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> deleteSection(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(admin.deleteSection(id), "Sección eliminada"));
    }

    // ------------------------------------------------ configuracion: campo

    @PostMapping("/sections/{sectionId}/fields")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> createField(
            @PathVariable UUID sectionId, @Valid @RequestBody FlowFieldRequest req) {
        return ResponseEntity.ok(ApiResponse.created(admin.createField(sectionId, req)));
    }

    @PutMapping("/fields/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> updateField(
            @PathVariable UUID id, @Valid @RequestBody FlowFieldRequest req) {
        return ResponseEntity.ok(ApiResponse.success(admin.updateField(id, req), "Campo guardado"));
    }

    @DeleteMapping("/fields/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> deleteField(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(admin.deleteField(id), "Campo eliminado"));
    }

    // ---------------------------------------------- configuracion: control

    @PostMapping("/sections/{sectionId}/controls")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> createControl(
            @PathVariable UUID sectionId, @Valid @RequestBody FlowControlRequest req) {
        return ResponseEntity.ok(ApiResponse.created(admin.createControl(sectionId, req)));
    }

    @PutMapping("/controls/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> updateControl(
            @PathVariable UUID id, @Valid @RequestBody FlowControlRequest req) {
        return ResponseEntity.ok(ApiResponse.success(admin.updateControl(id, req), "Control guardado"));
    }

    @DeleteMapping("/controls/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlowAdminView>> deleteControl(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(admin.deleteControl(id), "Control eliminado"));
    }

    /** Los roles del token. Sin sesion, sin permisos: se ven los botones libres. */
    private Set<String> roles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof IUserPrincipal p && p.getRoles() != null) {
            return p.getRoles();
        }
        return Set.of();
    }
}
