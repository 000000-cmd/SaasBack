package com.saas.events.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.dto.PagedResponse;
import com.saas.events.application.dto.response.NotificationStatsResponse;
import com.saas.events.application.dto.response.SendResponse;
import com.saas.events.application.service.StatsService;
import com.saas.events.domain.model.SendStatus;
import com.saas.events.domain.port.out.INotificationLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationStatsController {

    private final StatsService stats;
    private final INotificationLogRepositoryPort logs;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<NotificationStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.success(stats.get()));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<PagedResponse<SendResponse>>> logs(
            @RequestParam(required = false) SendStatus status,
            @RequestParam(required = false) String recipient,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = logs.search(status, recipient, page, size);
        PagedResponse<SendResponse> out = PagedResponse.<SendResponse>builder()
                .content(result.getContent().stream().map(SendResponse::from).toList())
                .page(result.getPage())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(out));
    }
}
