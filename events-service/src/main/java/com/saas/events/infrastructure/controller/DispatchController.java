package com.saas.events.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.events.application.dto.request.SendRequest;
import com.saas.events.application.dto.response.SendResponse;
import com.saas.events.application.service.DispatchService;
import com.saas.events.domain.model.NotificationLog;
import com.saas.events.infrastructure.client.ResendClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatch;

    /**
     * Camino sincrono, para el caso raro que no puede esperar al evento.
     * eventId nulo: la idempotencia protege la reentrega de Kafka, no el
     * "enviar otra vez" deliberado.
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<List<SendResponse>>> send(@Valid @RequestBody SendRequest req) {
        List<NotificationLog> written = dispatch.dispatch(req.notificationCode(),
                ResendClient.cleanRecipients(req.to()),
                req.data() == null ? Map.of() : req.data(),
                null);
        return ResponseEntity.ok(ApiResponse.success(written.stream().map(SendResponse::from).toList()));
    }
}
