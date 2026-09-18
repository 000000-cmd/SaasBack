package com.saas.system.infrastructure.controller.flow;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.response.flow.ResolvedFlowView;
import com.saas.system.application.service.flow.FlowMessageService;
import com.saas.system.application.service.flow.FlowResolverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * El flujo y sus textos, para quien no tiene sesion.
 *
 * <p>La web publica del negocio agenda sin cuenta, asi que no puede pedir el
 * flujo por el endpoint autenticado. Es el MISMO resolvedor: la unica
 * diferencia es que sin roles no hay permisos, y los controles que exigen uno
 * simplemente no salen. Un visitante no puede ver el boton de registrar una
 * cita retroactiva porque no tiene el permiso, no porque haya una segunda
 * copia de la configuracion para el publico.</p>
 *
 * <p>Cuelga de {@code /system/public}, que el gateway deja pasar sin token.</p>
 */
@RestController
@RequestMapping("/public/flows")
@RequiredArgsConstructor
public class PublicFlowController {

    private final FlowResolverService resolver;
    private final FlowMessageService messages;

    @GetMapping("/{code}/resolved")
    public ResponseEntity<ApiResponse<ResolvedFlowView>> resolved(
            @PathVariable String code,
            @RequestParam(defaultValue = "WEB") String channel) {
        // Sin roles: solo los pasos y botones que no exigen permiso.
        return ResponseEntity.ok(ApiResponse.success(resolver.resolve(code, channel, Set.of())));
    }

    /** Los textos de los pasos, por codigo. Los mismos que ve el panel. */
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<Map<String, String>>> messages() {
        return ResponseEntity.ok(ApiResponse.success(messages.byCode()));
    }
}
