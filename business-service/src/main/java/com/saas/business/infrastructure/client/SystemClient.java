package com.saas.business.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Feign a system-service para resolver permisos.
 *
 * <p>El token lleva ROLES, no permisos. Comprobar aqui "si es dueno, puede"
 * seria la comprobacion generica por rol que el diseno descarta: quien decide
 * que permisos tiene un rol es la configuracion, y vive en system-service.</p>
 *
 * <p>Solo endpoints {@code /internal/**} (S2S).</p>
 */
@FeignClient(name = "system-service", contextId = "system-internal-business", path = "/system")
public interface SystemClient {

    @PostMapping("/internal/permissions/by-role-codes")
    Set<String> permissionCodesByRoleCodes(@RequestBody Set<String> roleCodes);

    /**
     * El flujo resuelto para un canal. Lo usa la conversacion de WhatsApp, que
     * lee los MISMOS pasos que la web y el panel: si el dueno quita un paso, el
     * bot deja de preguntarlo sin tocar codigo.
     *
     * <p>Va por el endpoint publico porque el bot no tiene sesion. Sin roles no
     * hay permisos, y por eso no vienen los botones que exigen uno — que es
     * exactamente lo que corresponde a alguien escribiendo desde su telefono.</p>
     */
    @GetMapping("/public/flows/{code}/resolved")
    Sobre<ResolvedFlow> resolvedFlow(@PathVariable("code") String code,
                                     @RequestParam("channel") String channel);

    /** Los textos del flujo, por CODIGO. Es lo que el bot dice. */
    @GetMapping("/public/flows/messages")
    Sobre<Map<String, String>> flowMessages();

    /**
     * El sobre {@code ApiResponse} en el que viajan TODOS los endpoints
     * publicos del proyecto.
     *
     * <p>Sin el, Feign deserializa el sobre contra el objeto de dentro y todos
     * los campos salen NULOS — sin error, sin log. Los endpoints
     * {@code /internal/**} no lo llevan; los publicos, si.</p>
     */
    record Sobre<T>(boolean success, String message, T data) {}

    /** Espejo minimo de ResolvedFlowView: lo que la conversacion necesita. */
    record ResolvedFlow(String code, String name, String channel, List<Section> sections) {
        public record Section(String code, String name, String description, int step,
                              String kind, List<Field> fields, List<Control> controls) {}
        public record Field(String code, String label, String dataType, boolean required) {}
        public record Control(String code, String label, String action) {}
    }
}
