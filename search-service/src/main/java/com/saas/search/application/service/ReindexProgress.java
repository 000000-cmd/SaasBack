package com.saas.search.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Difusion del progreso del reindex a las pantallas conectadas.
 *
 * <p><b>Por que SSE y no WebSocket:</b> el cliente nunca envia nada, solo
 * escucha. SSE es HTTP normal — sin STOMP, sin handshake de upgrade que
 * configurar en el gateway, y se reconecta sin codigo extra. Un WebSocket seria
 * un canal bidireccional para un flujo que solo va en un sentido.
 *
 * <p>Se guarda un historial corto para que quien abra la pantalla con el
 * reindex ya empezado vea de inmediato lo que lleva, en vez de una consola en
 * blanco hasta el siguiente evento.
 */
@Slf4j
@Component
public class ReindexProgress {

    /** Eventos que se reenvian a quien se conecta tarde. */
    private static final int HISTORY_SIZE = 200;
    /** Sin latido, un proxy intermedio corta la conexion por inactividad. */
    private static final long HEARTBEAT_SECONDS = 20;
    /** Techo de vida de una suscripcion; el navegador reconecta si hace falta. */
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    /**
     * Un evento del recorrido.
     *
     * @param phase    start | progress | entity | end
     * @param scope    que se esta reindexando en conjunto ("todo" o la entidad)
     * @param entity   paso concreto ("locations:municipalities"), si aplica
     * @param done     registros procesados del paso
     * @param total    registros del paso (0 si aun no se sabe)
     * @param failed   fallos acumulados del paso
     * @param message  texto listo para pintar en el log
     */
    public record ProgressEvent(String phase, String scope, String entity,
                                long done, long total, long failed,
                                String message, Instant at) {}

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Deque<ProgressEvent> history = new ArrayDeque<>();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "reindex-sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    public ReindexProgress() {
        heartbeat.scheduleAtFixedRate(this::ping, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
    }

    /** Abre un canal y le manda de entrada lo que ya haya pasado. */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));

        List<ProgressEvent> replay;
        synchronized (history) {
            replay = List.copyOf(history);
        }
        try {
            for (ProgressEvent event : replay) {
                emitter.send(SseEmitter.event().name("progress").data(event));
            }
        } catch (IOException | IllegalStateException ex) {
            // El cliente se fue antes de terminar el replay: no se registra.
            return emitter;
        }

        emitters.add(emitter);
        return emitter;
    }

    public void start(String scope) {
        publish(new ProgressEvent("start", scope, null, 0, 0, 0,
                "Reindexado de " + scope + " iniciado", Instant.now()));
    }

    public void progress(String scope, String entity, long done, long total, long failed) {
        publish(new ProgressEvent("progress", scope, entity, done, total, failed,
                entity + ": " + done + "/" + total, Instant.now()));
    }

    public void entityDone(String scope, String entity, long indexed, long failed) {
        String message = failed > 0
                ? entity + " terminado: " + indexed + " indexados, " + failed + " con error"
                : entity + " terminado: " + indexed + " indexados";
        publish(new ProgressEvent("entity", scope, entity, indexed, indexed, failed, message, Instant.now()));
    }

    public void end(String scope, long indexed, long failed, long millis) {
        publish(new ProgressEvent("end", scope, null, indexed, indexed, failed,
                "Reindexado de " + scope + " completado en " + (millis / 1000) + "s: "
                        + indexed + " indexados, " + failed + " con error", Instant.now()));
    }

    public void failed(String scope, String reason) {
        publish(new ProgressEvent("end", scope, null, 0, 0, 1,
                "Reindexado de " + scope + " fallo: " + reason, Instant.now()));
    }

    /** Al empezar un recorrido nuevo se limpia el historial del anterior. */
    public void reset() {
        synchronized (history) {
            history.clear();
        }
    }

    private void publish(ProgressEvent event) {
        synchronized (history) {
            history.addLast(event);
            while (history.size() > HISTORY_SIZE) history.removeFirst();
        }
        send(SseEmitter.event().name("progress").data(event));
    }

    /** Comentario periodico: mantiene viva la conexion a traves del gateway. */
    private void ping() {
        if (emitters.isEmpty()) return;
        send(SseEmitter.event().comment("keepalive"));
    }

    private void send(SseEmitter.SseEventBuilder builder) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(builder);
            } catch (IOException | IllegalStateException ex) {
                // Cliente desconectado: se retira sin ruido, no es un error.
                emitters.remove(emitter);
            }
        }
    }
}
