package com.saas.search.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.search.application.dto.search.ReindexRequest;
import com.saas.search.application.service.ReindexProgress;
import com.saas.search.application.service.ReindexService;
import com.saas.search.application.service.ReindexService.IndexStatus;
import com.saas.search.application.service.ReindexService.ReindexResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Gestion de Elasticsearch. Sustituye al reindex que vivia escondido dentro de
 * la pantalla de terceros: aqui se ve el estado de TODOS los indices y se
 * reindexa con tres granularidades, todas por el mismo endpoint segun lo que
 * traiga el cuerpo (ver {@link ReindexRequest}).
 *
 * <pre>
 *   GET  /search/admin/indices                      -> fuente vs indexado
 *   POST /search/admin/reindex  {}                  -> todo
 *   POST /search/admin/reindex  {"entity":"roles"}  -> toda la entidad
 *   POST /search/admin/reindex  {"entity":"roles","id":"..."} -> un registro
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ElasticAdminController {

    private final ReindexService reindexService;
    private final ReindexProgress progress;

    /**
     * Progreso del reindex en vivo (SSE). Se emiten eventos {@code progress}
     * con fase, paso, procesados y total; quien se conecta con el trabajo ya
     * empezado recibe primero el historial.
     *
     * <p>El navegador NO usa {@code EventSource} aqui: no deja poner cabeceras y
     * el JWT acabaria en la URL. El front lo lee con {@code fetch} + stream.
     */
    @GetMapping(value = "/reindex/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public SseEmitter stream() {
        return progress.subscribe();
    }

    /** Estado de cada indice: cuanto hay en la fuente y cuanto en Elastic. */
    @GetMapping("/indices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<IndexStatus>>> indices() {
        return ResponseEntity.ok(ApiResponse.success(reindexService.status()));
    }

    /**
     * Respuesta del reindex. Un registro suelto se hace al momento y devuelve
     * sus cuentas; una entidad entera o todo se lanzan en segundo plano
     * ({@code background=true}) porque tardan minutos: quien llama vuelve a
     * pedir {@code /indices} para ver el avance.
     */
    public record ReindexResponse(boolean background, String scope, List<ReindexResult> results) {}

    /**
     * Reindexa. La granularidad la decide el cuerpo: sin entidad va todo, con
     * entidad va esa entidad completa, con entidad + id va solo ese registro.
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReindexResponse>> reindex(@RequestBody(required = false) ReindexRequest req) {
        ReindexRequest body = req == null ? new ReindexRequest(null, null) : req;

        if (body.hasEntity() && body.hasId()) {
            log.info("Reindex manual: {} id={}", body.entity(), body.id());
            ReindexResult result = reindexService.reindexOne(body.entity(), body.id());
            return ResponseEntity.ok(ApiResponse.success(
                    new ReindexResponse(false, body.entity(), List.of(result))));
        }

        String scope = body.hasEntity() ? body.entity() : "todo";
        log.info("Reindex manual en segundo plano: {}", scope);
        reindexService.reindexInBackground(body.hasEntity() ? body.entity() : null);
        return ResponseEntity.ok(ApiResponse.success(new ReindexResponse(true, scope, List.of())));
    }
}
