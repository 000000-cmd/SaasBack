package com.saas.events.infrastructure.controller;

import com.saas.common.security.IUserPrincipal;
import com.saas.events.application.service.InboxOwnerResolver;
import com.saas.events.infrastructure.realtime.InboxStreamHub;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * La bandeja en vivo.
 *
 * Se abre una vez al entrar y queda escuchando: cada notificación nueva llega
 * sola, sin que la campana tenga que preguntar. Es lo que sustituye al sondeo
 * cada pocos segundos que había antes — el mismo aviso, sin el goteo constante
 * de peticiones.
 *
 * El destinatario NO viaja en la URL: se resuelve del token. Así nadie puede
 * escuchar la bandeja de otro cambiando un identificador a mano.
 */
@RestController
@RequestMapping("/notification/stream")
@RequiredArgsConstructor
public class InboxStreamController {

    private final InboxStreamHub hub;
    private final InboxOwnerResolver owners;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal IUserPrincipal principal) throws IOException {
        UUID owner = owners.of(principal);
        if (owner == null) {
            // El administrador del sistema no tiene tercero y por tanto no tiene
            // bandeja. Se le contesta con una conexión que se cierra en vez de
            // un error: el cliente no tiene que saber por qué, y reintentar no
            // le va a servir de nada.
            SseEmitter vacio = new SseEmitter(0L);
            vacio.send(SseEmitter.event().name("ready").data(Map.of("ok", false)));
            vacio.complete();
            return vacio;
        }
        return hub.subscribe(owner);
    }
}
