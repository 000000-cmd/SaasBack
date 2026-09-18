package com.saas.events.infrastructure.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Empuje en vivo de la bandeja: quien tiene la web o la app abierta se entera
 * al instante, sin preguntar.
 *
 * <h3>Por qué SSE y no WebSocket</h3>
 * Lo que hace falta es UNA sola dirección: el servidor avisa, el cliente lee.
 * Nadie manda nada de vuelta por aquí — marcar como leída es un PUT normal.
 * Para eso, un WebSocket es un canal bidireccional, un broker STOMP, una
 * dependencia más en el back y una librería más en Flutter, todo para no usar
 * la mitad. SSE viaja sobre HTTP corriente: cruza el gateway, cruza el túnel de
 * Cloudflare, se reconecta solo y no añade una sola dependencia.
 *
 * Y pesa menos, que en este proyecto no es un detalle: la campana estaba
 * preguntando "¿hay algo nuevo?" cada pocos segundos, para todos los usuarios
 * conectados, casi siempre para oír que no. Una conexión abierta y en silencio
 * gasta menos banda que ese sondeo.
 *
 * <h3>Latidos</h3>
 * Cada 25 segundos sale un comentario SSE (una línea que empieza por dos
 * puntos, que el cliente ignora). No es decorativo: Cloudflare y cualquier
 * proxy intermedio cierran una conexión que lleva callada demasiado tiempo, y
 * la caída se descubriría al primer aviso perdido. De paso, es lo que detecta
 * los emisores muertos y los saca del mapa.
 *
 * ponytail: el registro vive en la memoria de ESTE proceso. Con una instancia
 * de events-service —que es como corre hoy— es correcto y es gratis. Con dos,
 * un aviso emitido en la instancia A no llega a quien está conectado a la B;
 * el sitio del cambio es {@link #publish}, publicando a un canal de Redis
 * (ya está en el stack) al que ambas se suscriban.
 */
@Slf4j
@Component
public class InboxStreamHub {

    /** Media hora por conexión. Al vencer, el cliente reconecta solo. */
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    /**
     * Una persona puede tener varias: el navegador del escritorio, otra pestaña
     * y el móvil. Todas reciben.
     */
    private final Map<UUID, List<SseEmitter>> porDuenio = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID owner) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        porDuenio.computeIfAbsent(owner, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> quitar(owner, emitter));
        emitter.onTimeout(() -> quitar(owner, emitter));
        emitter.onError(e -> quitar(owner, emitter));

        // Primer mensaje inmediato. Sin él, algunos proxies retienen la
        // respuesta hasta juntar bytes suficientes y la conexión parece colgada
        // durante los primeros segundos.
        try {
            emitter.send(SseEmitter.event().name("ready").data(Map.of("ok", true)));
        } catch (IOException ex) {
            quitar(owner, emitter);
        }
        return emitter;
    }

    /**
     * Avisa a todas las conexiones de una persona.
     *
     * Nunca lanza: esto se llama DENTRO del despacho de una notificación, y que
     * un navegador se haya ido no puede tumbar el envío del correo.
     */
    public void publish(UUID owner, String event, Object payload) {
        List<SseEmitter> lista = porDuenio.get(owner);
        if (lista == null || lista.isEmpty()) return;
        for (SseEmitter e : lista) {
            try {
                e.send(SseEmitter.event().name(event).data(payload));
            } catch (Exception ex) {
                quitar(owner, e);
            }
        }
    }

    /** Cuántas conexiones vivas hay (para el panel de estado del subsistema). */
    public int connections() {
        return porDuenio.values().stream().mapToInt(List::size).sum();
    }

    @Scheduled(fixedDelay = 25_000L)
    void heartbeat() {
        porDuenio.forEach((owner, lista) -> {
            for (SseEmitter e : lista) {
                try {
                    e.send(SseEmitter.event().comment("hb"));
                } catch (Exception ex) {
                    quitar(owner, e);
                }
            }
        });
    }

    private void quitar(UUID owner, SseEmitter emitter) {
        List<SseEmitter> lista = porDuenio.get(owner);
        if (lista == null) return;
        lista.remove(emitter);
        // Sin esto el mapa acumularía una lista vacía por cada persona que
        // alguna vez se conectó, y nunca se vaciaría.
        if (lista.isEmpty()) porDuenio.remove(owner, lista);
    }
}
