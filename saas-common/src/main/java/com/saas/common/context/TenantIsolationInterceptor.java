package com.saas.common.context;

import com.saas.common.security.IUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * El aislamiento entre negocios.
 *
 * <h3>Que problema resuelve</h3>
 * Toda consulta acotada por negocio tomaba el identificador de la peticion:
 * {@code GET /finance/service-charges/history?businessId=...}. El
 * {@code businessId} viajaba firmado en el JWT, pero {@code IUserPrincipal} no
 * lo exponia, asi que nadie podia compararlo. Resultado: cualquier usuario
 * autenticado leia los datos de otro negocio cambiando un UUID. Con la agenda
 * eso deja de ser "ver cifras ajenas" y pasa a ser "ver los telefonos de los
 * clientes de otro".
 *
 * <h3>Como lo resuelve</h3>
 * Si el token trae negocio y la peticion trae otro, se responde 403. No hay que
 * tocar ni un controlador: lo que llega por parametro o por variable de ruta se
 * compara aqui, para todos los servicios que escanean {@code com.saas.common}.
 *
 * <p>Es un interceptor y no un filtro a proposito: un filtro corre ANTES de la
 * cadena de seguridad —el principal todavia no existe— y ademas no ve las
 * variables de ruta ya resueltas. Aqui hay las dos cosas.</p>
 *
 * <h3>Quien pasa</h3>
 * <ul>
 *   <li>El administrador del sistema: su token no trae negocio porque no
 *       pertenece a ninguno, y opera sobre todos.</li>
 *   <li>Lo publico sin sesion (la pagina del negocio, la reserva por
 *       subdominio): no hay principal contra el que comparar, y esos
 *       endpoints resuelven el negocio por el host, no por parametro.</li>
 * </ul>
 *
 * <p>El header {@code X-Business-Id} deja de ser fuente de verdad: se sigue
 * aceptando por compatibilidad, pero se comprueba igual y el contexto se
 * rellena desde el TOKEN.</p>
 */
@Slf4j
public class TenantIsolationInterceptor implements HandlerInterceptor {

    /** El nombre con el que el negocio viaja, sea parametro o variable de ruta. */
    private static final String PARAM = "businessId";

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UUID delToken = businessOfCaller();

        // Sin negocio en el token: administrador del sistema o peticion
        // publica. Ninguno de los dos se acota por este mecanismo.
        if (delToken == null) return true;

        // Desde aqui, el negocio del token MANDA sobre cualquier otro origen.
        BusinessContext.set(delToken);

        UUID pedido = primeroNoNulo(
                uuidOrNull(request.getParameter(PARAM)),
                uuidOrNull(request.getHeader(BusinessContextFilter.HEADER)),
                uuidOrNull(varDeRuta(request)));

        if (pedido != null && !pedido.equals(delToken)) {
            log.warn("Aislamiento: {} pidio el negocio {} y el suyo es {}",
                    request.getRequestURI(), pedido, delToken);
            responder403(response);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // El contexto vive en un ThreadLocal y los hilos se reutilizan: sin
        // esta limpieza, la siguiente peticion del mismo hilo heredaria el
        // negocio de la anterior.
        BusinessContext.clear();
    }

    private static UUID businessOfCaller() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof IUserPrincipal p)) return null;
        return p.getBusinessId();
    }

    @SuppressWarnings("unchecked")
    private static String varDeRuta(HttpServletRequest request) {
        Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(vars instanceof Map<?, ?> map)) return null;
        Object v = ((Map<String, String>) map).get(PARAM);
        return v == null ? null : v.toString();
    }

    private static UUID primeroNoNulo(UUID... valores) {
        for (UUID v : valores) {
            if (v != null) return v;
        }
        return null;
    }

    private static UUID uuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * 403 con la forma de {@code ApiResponse} para que el front lo lea igual
     * que cualquier otro error, sin un caso especial.
     */
    private static void responder403(HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        String cuerpo = "{\"success\":false,"
                + "\"message\":\"No tienes acceso a los datos de ese negocio\","
                + "\"status\":403}";
        try {
            response.getOutputStream().write(cuerpo.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            // La respuesta ya se cerro; el 403 esta puesto y es lo que importa.
        }
    }
}
